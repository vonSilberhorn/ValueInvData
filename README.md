# ValueInvestorData 

This is a personal demo repo, none of the projects here are intended to be used in actual production settings. 
The purpose of this repo is simply to develop and showcase my coding, code design and system design skills.
The idea for the domain of the repo comes from my personal finance background and my continuing passion for investments, especially 
the value investing genre: [Value investing explained on Investopedia](https://www.investopedia.com/terms/v/valueinvesting.asp)

Despite the domain though, it's more about technical stuff than providing any real actionable input for value investing decision - at least for now :)

I have set up a multi-module project in the repo, using the BOM model: dependency versions, including the ones from within the repo, are only declared in the [Bill Of Materials](pom.xml), every module in the project inherits the dependency versions from here, ensuring consistency. This is only theoretical for now though, as the repo only has one real project, which is the:

## StockValuationService
[Link to the module](services/StockValuationService)


Currently, this is the only actual project in this would-be multimodule repo.
This is a simple http server generating valuation reports on stocks. It handles one saga - business process - and that is 
when a request comes in, referencing a ticker of a stock, e.g. 'AAPL' for Apple, it generates a custom valuation report with educational purpose recommendations.

The report includes a discounted cash flow valuation, compared to the current stock price, and summary reports on stock analysts' price targets for the stock, giving a good overview on whether the given stock is generally undervalued, fairly valued, or overvalued.
There are also Investopedia links in the reports for further readings on what all this data means, for those who are interested.

More on how to actually run it, further down below.

### Design choices

The code is in **Java**, language level **21(LTS)**. Java is my main language, I wrote the service from scratch within 6 days, it wouldn't have been possible for me to achieve the same thing in another language. I hope to use other languages too in future projects though.


**No frameworks**: I did not use any frameworks such as Spring or Hibernate. This has of course forced me to write a lot more lines of code than I would have needed otherwise, but I chose to do this precisely for that reason. Because it also means
that I had to deal with lower level stuff that is normally hidden by these frameworks. A good example is the ton of code in the [persistence package](services/StockValuationService/src/main/java/com/szilberhornz/valueinvdata/services/stockvaluation/persistence) I had to write that would have been a few annotations with Spring. For a production service, I would have probably gone with 
Spring, but again, this is a demo project, and I chose to practice and demo my understanding of lower level processes (that is, lower level only from an application perspective)

**Rough overview**: the app implements an [orchestrated saga](services/StockValuationService/src/main/java/com/szilberhornz/valueinvdata/services/stockvaluation/valuationreport/VRSagaOrchestrator.java) for the only business process it has, which is called ValuationReport. 
The orchestrated saga is mainly a microservice pattern, but splitting this app into multiple microservices at this point would probably be a huge overkill. Not that I didn't implement huge overkills, but more on those later. So the process of the saga looks like this: after receiving the request, the orchestrator first looks at the 1) in-memory cache, then in case of missing or incomplete data, the 2) the database, which is either an in-memory H2DB or an externally running MSSQL instance, and as a fallback, to the 3) [Financial Modeling Prep Api](https://financialmodelingprep.com/developer/docs/), 
which is basically the source of all actual market data. Then as step 4), it writes back all data to the database and the cache. The orchestrator also uses a very simple circuit breaker logic, which is basically implementing timeouts.

The process and the entire application heavily relies on asynchronous and parallel execution, which is one of the overkills I implemented, since it never going to need it, but again, it's a demo. The server starts with a thread pool of 10 to begin with, but all the data collection and writes happen in async with the extensive usage of the CompletableFuture java api. 

The other overkill I implemented is a [Least Frequently Used pattern cache with async eviction](services/StockValuationService/src/main/java/com/szilberhornz/valueinvdata/services/stockvaluation/cache/ValuationServerLFUCache.java). This allows O(1) insertions, while the rebalance of the Tree responsible for frequency mapping, and the eviction only happens periodically and asynchronously. 

I also added two possible DataSource implementations - as already mentioned briefly above - an in-memory db using H2DB, for which the initializer queries can be found in the standard resources folder, and one for an externally running MSSQL instance, which the app assumes it is properly setup if used. Both DataSources are backed by HikariCP connection pools, which are probably the best you can find out there. Connection pools help greatly reduce the number of physical connection instantiations on the databases, and also perfect for parallel query execution, which I also take advantage of in the app code.

To defend against malicious user input, I leveraged the fact that there aren't an infinite number of tickers in existence. So I simply downloaded ALL existing tickers in a file (less than 1 MB) and the app reads and caches all of those in the [Ticker Cache](services/StockValuationService/src/main/java/com/szilberhornz/valueinvdata/services/stockvaluation/cache/TickerCache.java). If someone sends a request with a ticker that is not in the cache, the service returns an HTTP 403

*Other notes* I tried to implement production-level exception handling and logging, adding javadocs and even comments about my intentions wherever I felt it was needed. 
For the https client talking to the FMP api, I used the "new" http client library available since JDK11. I also wrote more than 120 unit tests - as it stands at the time of writing this. 
And last but not least, I also used Sonar reports as an IDE plugin and as a GitHub Action to help me maintain high code quality standards.

**Known shortcomings** One that sticks out the most is that the data the app generates the reports from is pretty static. 
I chose a topic that is not too dynamic in itself, as valuations don't change daily, so even if the "current" stock price in the valuation is stale, 
the rest of it is pretty relevant for like a month or so. Yet, ideally cached items shouldn't live more than a day and the database entries should also be updated at least daily. 

The other is a lack of containerization, which I plan to work on soon.

### How to run the service 

#### In IDE

Since containerizing this app is a next step, for now even I only ran it in IDE - I use IntelliJ. 
One of the most important things to note is that to access the Financial Modeling Prep api, you need an api key. As I noted in the code, this would be the responsibility of the app to provide through 
a secret store like Vault. But since it's still just a demo, you are better off generating a free key at [the FMP website](https://site.financialmodelingprep.com/developer/docs). You can then use the -DFMP_API_KEY=KEY VM Option where KEY is the api key string.

If you don't want to get an own api key, you can run queries for tickers that are preloaded in the in-memory database, these are AAPL, MSFT, AMZN, NVDA, META, TSM, LLY, ORCL, ASML and partial data on CSCO.
Even if you get an api key, you will not be able to access actual reports from the price target summary and price target consensus 
(those are not free to use), but the app is written in a way to return partial data and possible notices on api key issues. Also, the free api key is rate limited to 250 calls/day, but it's probably enough for demo purposes.

If you want to use MSSQL instead, you will need to set up an own instance (SQL statements from the resources folder may help) and provide the address with the 
-DMSSQL_ADDRESS=... VM Options. You also need to pass a pw and a username of course -DMSSQL_USER=... and -DMSSQL_PW=...

The app generates a human-readable form by default - demo purposes of course - but it's also possible to change that to the standard JSON output with the 
-DVALUATION_REPORT_FORMAT=JSON VM Option.

You can check more possible configurations in the [AppContext class](services/StockValuationService/src/main/java/com/szilberhornz/valueinvdata/services/stockvaluation/AppContext.java)

After starting the service, you can use a browser or any other tool to call http://localhost:8080/valuation-report?ticker=AAPL to see the output, where AAPL can be changed to any other valid ticker.

That's it!


#### Docker

// this is a todo item. As one of the next steps, I plan to containerize the app to makes sure it could run anywhere without the need for anyone to build it locally.

## What's next

I plan to write a load balancer and a rate limiter (probably two in one). Not because it makes sense to write custom API Gateways for prod purposes, only purely to learn and practice.

I also plan to implement a **choreographed saga** after the orchestrated one. But this time I will have to have multiple microservices as I plan to leverage Kafka, likely with protobuf message definitions.