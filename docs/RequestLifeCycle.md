Nocturne only processes requests that match the url-pattern for the org.nocturne.main.DispatchFilter.

If the request URL matches the nocturne.skip-regex parameter, Nocturne stops processing the request and passes it to the filterChain.

Otherwise, the mechanism for finding a suitable page (Page) to handle the request is initiated. For this, the URL and request.getParameterMap() are passed to the class specified in nocturne.request-router, which returns a special Resolution object with three fields: pageClassName, action, and overrideParameters. The first denotes the page (controller) class name, the second the action within that page, and the third is a Map that overrides (or adds) parameters to the request.

Once the Page is identified, an instance of the page is either created or taken from the page pool, and then work proceeds with it. Note that pages are allocated from the pool in such a way that a single page instance is not processed simultaneously by two or more threads. Thus, there is no need to worry about thread safety for pages.

Next:

1. The beforeProcessPage() methods of the registered PageRequestListeners are called.

2. If the page was just created, its init() method is called. Therefore, this method is called exactly once for each page (upon the first request after creation).

3. The initializeAction() method is called (regardless of the expected action). At this point, the page is ready for use, and parameters have been injected into it.

4. The Events.beforeAction() event is generated, and all subscribers to this class of the page or its ancestor will be notified.

5. The appropriate validation method for the action is called. This is either a method named validate() or a method annotated with @Validate("action name"). If the annotation is specified without a parameter, the annotated method becomes the default. If the required action is not found, no method is called. For details on validation, see [ValidationFlow](ValidationFlow.md). If the validation method returns true, control proceeds to the first of the next two items; otherwise, it proceeds to the second:

    * The required action is called: this is either the action() method (by default) or any void method with an empty parameter list annotated with @Action("action name"). If the annotation is specified without a parameter, the annotated method becomes the default. If the required action is not found, the default action is called. Pages may parse frames at this stage (though they can also do so at other stages). For more information, see [Frames](Frames.md).

    * The required invalid method is called: this is either the invalid() method (by default) or any void method with an empty parameter list annotated with @Invalid("action name"). If the annotation is specified without a parameter, the annotated method becomes the default. If the required invalid method is not found, the default invalid method is called. If that is also absent, nothing is called.

6. The Events.afterAction() event is generated, and all subscribers to this class of the page or its ancestor will be notified.

7. The finalizeAction() method is called.

8. If the page has not refused template processing (i.e., has not called skipTemplate()), the template processing is initiated. By default, the template name matches the short class name of the page plus the ".ftl" suffix, but the name can be changed using setTemplateName.

9. The afterProcessPage() methods of the registered PageRequestListeners are called.

10. If the page has called the setProcessChain(true) method, the request-response pair is passed to the filterChain.

If an exception occurs, control is transferred to step 9, but step 10 is skipped. The abortXXX() methods trigger an AbortException, after writing the corresponding redirection to the response.
