# Dronazon

### Introduction
The purpose of the project is to create a system that manages the deliveries of orders received from the Dronazon e-commerce site, through the use of drones.

(project done for the Distributed and Pervasive System course of Unimi) 

### Description

Dronazon has a its disposal a set of drones whitin a smart city, these drones need to elect a master in a decentralized manner, when a customer places a new order to Dronazon, this request is passed to the master-drone who decides which drone will have to manage it. 
Every drone has a battery, with each delivery the battery level drops, when battery level is below 15% the drone has to leave the system. A drone may require to charge, but only one drone can charge its battery at a time. Every drone has a sensor that measures air pollution on the way to place the order. 

Periodically the net of drones has to communicate its statistics to a remote server, Administrator Server. Dronazon admins can manage the system via Administrator Server, viewing stats, adding or removing a drone.

### Components

- Drone: simulated by a process, uses Grpc to communicate to other drones
- Dronazon: MQTT publisher that simulates an e-commerce site, generating new deliveries 
- Administrator Server: REST server that receives statistics from drones and allows management of the system
- Administrator Client: client to query the server administrator for system status

### Decentralized network
The network has a ring structure, the initialization must take place in a decentralized way whitout the partecipation of the Administrator Server. 
The election of a new master is done by using Chang and Robert algorithm, the request of recharge is manage by Ricart and Agrawala algorithm.

For more detail information see Report.PDF
