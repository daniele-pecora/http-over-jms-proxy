# Quickstart for Linux

1. Open terminal in working directory
    ```
        cd dist/instances
    ```
2. Set permissions
    ```
        $ chmod 755 ./fixperm.sh && ./fixperm.sh
    ```
3. Set target url
    ```
        $ ./set-url.sh https://angular.io
    ```

4. Set broker address
    ```
        $ ./set-broker.sh localhost:61616
    ```

5. start **_broker_** instance 
    ```
        $ ./broker.sh start
    ```
    
6. start **_DMZ_** instance
    ```
        $ ./dmz.sh start
    ```
    
7. start **_internet_** instance
    ```
        $ ./internet.sh start
    ```

_All instances can be stopped by using the same command but with the argument `stop` instead._
