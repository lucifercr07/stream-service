# stream-service
#### Server:
1. Will be able to register new clients with unique username and password. `DONE`
2. Verify clients and handover token to them. `DONE`
3. Implement task scheduler. []
        i. Use executor service or rxjava to implement task scheduler.
        ii. It’s job will be to take task from service authentication, alert, rules etc. and store them         to be executed later as per the specified timeframe.
        iii. Task scheduler will have functionality for now to create, execute and delete scheduled task. Updation of once scheduled task will not be supported.
4. Collect alerts from client and store it as a task. []
5. Alert service will pick up alerts and do required action. []



#### Client:
1. Client will be edge streaming client which will execute rules defined from server on metrics being collected. []
2. Depending on rules execution result alerts will be raised and will be sent to the server. []
3. Client will register with a username and password with the server. []
4. It will be able to collect metrics via api [] or mqtt [].
5. Client will apply simple rules on metric for now and send alert to server. []
* (Will reside in `scripts` dir) 

#### Technology used:
1. Language: Java
2. DB: Mongo
3. Authentication mech.: jwt
4. Task scheduler mech.: TBD
5. Client language: golang