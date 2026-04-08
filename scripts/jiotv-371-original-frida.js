Java.perform(function () {
  function findFactoryFor(className) {
    var loaders = Java.enumerateClassLoadersSync();
    for (var i = 0; i < loaders.length; i++) {
      try {
        var factory = Java.ClassFactory.get(loaders[i]);
        factory.use(className);
        console.log('[loader] found ' + className + ' in loader #' + i + ' -> ' + loaders[i]);
        return factory;
      } catch (err) {}
    }
    return Java;
  }

  function tryHook(className, methodName, implFactory) {
    try {
      var factory = findFactoryFor(className);
      var klass = factory.use(className);
      if (!klass[methodName]) {
        console.log('[skip] ' + className + '.' + methodName + ' missing');
        return;
      }
      var overloads = klass[methodName].overloads;
      overloads.forEach(function (overload, idx) {
        overload.implementation = implFactory(className, methodName, overload, idx);
      });
      console.log('[hooked] ' + className + '.' + methodName + ' (' + overloads.length + ' overloads)');
    } catch (err) {
      console.log('[skip] ' + className + '.' + methodName + ' -> ' + err);
    }
  }

  tryHook('com.pairip.licensecheck3.LicenseClientV3', 'handleError', function () {
    return function () {
      console.log('[bypass] LicenseClientV3.handleError(' + arguments.length + ')');
      return;
    };
  });

  tryHook('com.pairip.licensecheck3.LicenseClientV3', 'connectToLicensingService', function (className, methodName, overload) {
    return function () {
      console.log('[trace] LicenseClientV3.connectToLicensingService');
      try {
        return overload.apply(this, arguments);
      } catch (err) {
        console.log('[trace] connectToLicensingService threw: ' + err);
        throw err;
      }
    };
  });

  tryHook('android.content.ContextWrapper', 'startActivity', function (className, methodName, overload) {
    return function (intent) {
      try {
        if (intent) {
          console.log('[intent] startActivity -> ' + intent);
        }
      } catch (err) {
        console.log('[intent] logging failed: ' + err);
      }
      return overload.apply(this, arguments);
    };
  });

  console.log('[ready] JioTV 371 original-split Frida hooks installed');
});
