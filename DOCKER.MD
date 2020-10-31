#  Run as Docker

1. Create image

    Make sure you have [build the distribution](./CONFIGURATION.MD#Build) directory via maven (`mvn clean package`) before creating the image
    
    ```
        $ docker build . -t jms-proxy
    ```
    
2. Start Broker Container
   ```
        $ docker run --rm -t -i -p 8161:8161 -p 61616:61616 --name jms-proxy-broker jms-proxy:latest bash -c "/HTTP2JMS/jms-proxy/instances/broker.sh start && bash"
   ```
   use this instead if you want to have loggin in terminal
   ```
        $ docker run --rm -t -i -p 8161:8161 -p 61616:61616 --name jms-proxy-broker jms-proxy:latest bash -c "/HTTP2JMS/jms-proxy/instances/broker.sh start && tail -f /HTTP2JMS/jms-proxy/instances/internet/apache-activemq-5.15.7-broker/data/activemq.log"
        
   ```
    Open broker admin page with access **admin:admin** at http://localhost:8161/admin/
    

3. Start Consumer (DMZ) Container

    Get your local IP (e.g with this command `$ ifconfig | grep 192.`)  
    and place at placeholder `<enter-your-local-IP-here>`
    ```
        $ docker run --rm -t -i --add-host http2jms-broker:<enter-your-local-IP-here> --name jms-proxy-consumer -e "JMS_BROKER_DESTINATION=http2jms-broker:61616" -e "JMS_TARGET_URI=https://angular.io" jms-proxy:latest /HTTP2JMS/jms-proxy/instances/dmz.sh run
        
    ```

4. Start Producer (Internet) Container

    Get your local IP (e.g with this command `$ ifconfig | grep 192.`)  
    and place at placeholder `<enter-your-local-IP-here>`

    ```
        $ docker run --rm -t -i --add-host http2jms-broker:<enter-your-local-IP-here> -p 9374:9374 --name jms-proxy-producer -e "JMS_BROKER_DESTINATION=http2jms-broker:61616" -e "JMS_TARGET_URI=https://angular.io" jms-proxy:latest /HTTP2JMS/jms-proxy/instances/internet.sh run 
        
    ```
    
6. Open browser
   Open in browser https://localhost:9374/

7. Clear all
    When ready you might want to remove the docker images and container 
    ```
        docker rm -f $(docker ps -a --filter "label=shortname=HTMLoverJMS") && docker rmi -f $(docker images --filter "label=shortname=HTMLoverJMS" -q)
    
    ```
