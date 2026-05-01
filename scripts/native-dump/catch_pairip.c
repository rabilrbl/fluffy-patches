/*
 * catch_pairip.c — Freeze JioTV during pairipcore's brief load window
 * 
 * Strategy: Use the "zygote wait" technique:
 * 1. Kill existing JioTV
 * 2. Start JioTV via am start
 * 3. Immediately poll for the new PID
 * 4. PTRACE_ATTACH as fast as possible
 * 5. Single-step, checking maps each step for pairipcore
 * 
 * Alternative: Use SIGSTOP race
 * 1. Start two threads: one polls for PID, one sends SIGSTOP
 * 2. As soon as PID appears, SIGSTOP it before pairipcore unloads
 * 
 * This version uses rapid SIGSTOP on PID detection.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <signal.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <dirent.h>
#include <time.h>

#define MAX_REGIONS 64

static volatile int target_pid = 0;

static int find_pid(const char *package) {
    DIR *dir;
    struct dirent *entry;
    char path[256], cmdline[256];
    int fd;
    ssize_t n;
    int pid;

    dir = opendir("/proc");
    if (!dir) return -1;

    while ((entry = readdir(dir)) != NULL) {
        pid = atoi(entry->d_name);
        if (pid <= 0) continue;

        snprintf(path, sizeof(path), "/proc/%d/cmdline", pid);
        fd = open(path, O_RDONLY);
        if (fd < 0) continue;

        n = read(fd, cmdline, sizeof(cmdline) - 1);
        close(fd);
        if (n <= 0) continue;
        cmdline[n] = '\0';

        if (strstr(cmdline, package)) {
            closedir(dir);
            return pid;
        }
    }
    closedir(dir);
    return -1;
}

struct mem_region {
    unsigned long start;
    unsigned long end;
    char perms[8];
    unsigned long offset;
    char path[1024];
};

static int get_library_regions(int pid, const char *lib_pattern, 
                                struct mem_region *regions, int max_regions) {
    char maps_path[256];
    char line[4096];
    FILE *fp;
    int count = 0;
    unsigned long start, end;
    char perms[8];
    unsigned long offset;
    int dev_major, dev_minor;
    unsigned long inode;
    char pathname[1024];

    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    fp = fopen(maps_path, "r");
    if (!fp) return -1;

    while (fgets(line, sizeof(line), fp) && count < max_regions) {
        int n = sscanf(line, "%lx-%lx %4s %lx %x:%x %lu %1023[^\n]",
                       &start, &end, perms, &offset, &dev_major, &dev_minor, &inode, pathname);
        if (n >= 7 && strstr(pathname, lib_pattern)) {
            regions[count].start = start;
            regions[count].end = end;
            strncpy(regions[count].perms, perms, sizeof(regions[count].perms) - 1);
            regions[count].offset = offset;
            strncpy(regions[count].path, pathname, sizeof(regions[count].path) - 1);
            count++;
        }
    }
    fclose(fp);
    return count;
}

static int dump_region(int pid, unsigned long start, unsigned long end, 
                        const char *output_path) {
    char mem_path[256];
    int mem_fd, out_fd;
    unsigned char *buf;
    unsigned long size;
    ssize_t n_read;

    snprintf(mem_path, sizeof(mem_path), "/proc/%d/mem", pid);

    mem_fd = open(mem_path, O_RDONLY);
    if (mem_fd < 0) {
        fprintf(stderr, "Cannot open %s: %s\n", mem_path, strerror(errno));
        return -1;
    }

    if (lseek(mem_fd, (off_t)start, SEEK_SET) < 0) {
        fprintf(stderr, "Cannot seek to 0x%lx: %s\n", start, strerror(errno));
        close(mem_fd);
        return -1;
    }

    size = end - start;
    buf = malloc(size);
    if (!buf) {
        close(mem_fd);
        return -1;
    }

    n_read = read(mem_fd, buf, size);
    close(mem_fd);

    if (n_read < 0) {
        fprintf(stderr, "Cannot read memory: %s\n", strerror(errno));
        free(buf);
        return -1;
    }

    out_fd = open(output_path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (out_fd < 0) {
        fprintf(stderr, "Cannot open output: %s\n", strerror(errno));
        free(buf);
        return -1;
    }

    write(out_fd, buf, n_read);
    close(out_fd);

    fprintf(stderr, "  -> Dumped %zd bytes from 0x%lx-0x%lx to %s\n", 
            n_read, start, end, output_path);
    free(buf);
    return 0;
}

int main(int argc, char *argv[]) {
    const char *package = "com.jio.jioplay.tv";
    const char *lib_pattern = "pairipcore";
    const char *output_dir = "/data/local/tmp";
    int pid;
    struct mem_region regions[MAX_REGIONS];
    int n_regions, i, attempt;
    int attached = 0;
    long ret;

    if (argc > 1) package = argv[1];
    if (argc > 2) output_dir = argv[2];

    fprintf(stderr, "[*] catch_pairip - Rapid ptrace dumper\n");
    fprintf(stderr, "[*] Target: %s\n", package);

    /* Step 1: Find running process or wait for it */
    pid = find_pid(package);
    if (pid <= 0) {
        fprintf(stderr, "[-] Process not found. Starting it...\n");
        /* Caller should start the app first */
        return 1;
    }
    fprintf(stderr, "[+] Found PID: %d\n", pid);

    /* Step 2: Attach with ptrace IMMEDIATELY */
    ret = ptrace(PTRACE_ATTACH, pid, NULL, NULL);
    if (ret < 0) {
        fprintf(stderr, "[-] PTRACE_ATTACH failed: %s\n", strerror(errno));
        return 1;
    }
    attached = 1;

    int status;
    waitpid(pid, &status, 0);
    fprintf(stderr, "[+] Attached (status=0x%x), checking maps...\n", status);

    /* Step 3: Check for pairipcore - might already be loaded during early init */
    n_regions = get_library_regions(pid, lib_pattern, regions, MAX_REGIONS);
    if (n_regions > 0) {
        fprintf(stderr, "[+] FOUND libpairipcore.so with %d regions!\n", n_regions);
        goto dump;
    }

    /* Step 4: Rapid cycle - continue briefly, check maps, repeat */
    fprintf(stderr, "[*] Library not in maps yet. Rapid polling...\n");
    
    for (attempt = 0; attempt < 500; attempt++) {
        /* Continue for a tiny bit then re-stop */
        ptrace(PTRACE_CONT, pid, NULL, NULL);
        usleep(500);  /* 0.5ms */
        
        /* Send SIGSTOP */
        kill(pid, SIGSTOP);
        usleep(1000); /* 1ms to ensure stop */
        
        /* Wait for stop */
        waitpid(pid, &status, __WALL);
        
        n_regions = get_library_regions(pid, lib_pattern, regions, MAX_REGIONS);
        if (n_regions > 0) {
            fprintf(stderr, "[+] FOUND after %d attempts (%.1fms)!\n", 
                    attempt + 1, (attempt + 1) * 1.5);
            goto dump;
        }
    }
    
    fprintf(stderr, "[-] Library not found after 500 attempts\n");
    goto cleanup;

dump:
    fprintf(stderr, "[+] Found %d regions for %s\n", n_regions, lib_pattern);
    
    unsigned long first_start = 0, last_end = 0;
    char output_path[512];
    
    for (i = 0; i < n_regions; i++) {
        fprintf(stderr, "  Region %d: 0x%lx-0x%lx %s offset=0x%lx\n",
                i, regions[i].start, regions[i].end, 
                regions[i].perms, regions[i].offset);
        
        if (first_start == 0 || regions[i].start < first_start)
            first_start = regions[i].start;
        if (regions[i].end > last_end)
            last_end = regions[i].end;

        /* Dump each individual executable region */
        if (strstr(regions[i].perms, "x")) {
            snprintf(output_path, sizeof(output_path), 
                     "%s/pairipcore_code_0x%lx.bin", output_dir, regions[i].start);
            dump_region(pid, regions[i].start, regions[i].end, output_path);
        }
    }

    /* Dump full span of all regions */
    if (first_start > 0 && last_end > first_start) {
        snprintf(output_path, sizeof(output_path), "%s/pairipcore_dump.so", output_dir);
        fprintf(stderr, "\n[*] Dumping full region 0x%lx-0x%lx (%lu bytes)\n",
                first_start, last_end, last_end - first_start);
        dump_region(pid, first_start, last_end, output_path);
    }

cleanup:
    if (attached) {
        ptrace(PTRACE_DETACH, pid, NULL, NULL);
        fprintf(stderr, "[*] Detached\n");
    }
    return (n_regions > 0) ? 0 : 1;
}