# test
Kafka Broker IP Restrictions:

Configure Kafka to only accept requests from the input service's IP address. This means any direct request to Kafka that does not go through the input service will be blocked.
Network-Level IP Whitelisting:

You can achieve this by setting up firewall rules or network access control lists (ACLs) that only allow traffic from the IP address of the input service to the Kafka brokers. Here is how you can implement this:
