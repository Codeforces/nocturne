### Using PageRequestListeners

You can register listeners by specifying a list of fully qualified class names in nocturne.page-request-listeners. Classes must be separated by semicolons and should implement PageRequestListener. These classes are instantiated with IoC support (i.e., you can use @Inject in them). Each object is notified before page processing and after it. If processing results in an exception, the corresponding exception is passed as the second parameter in afterProcessPage.
