# CIS 630 Project Status Summary #

## Project ##
Network Simulation

## Description ##
Goal of the project is to simulate a network with computers
and routers and messages being send and received between them.

## Project Evolution ##
Simulation of a botnet was our initial goal.  In order to contain and run a botnet, we chose to implement a distributed network simulation.  Due to the complexity of the process, building a network simulation became a project in itself and the botnet fell by the wayside.  Within the network simulation framework we were able to implement the Routing Information Protocol, the Token Ring Algorithm, and the Bully Algorithm which employs Lamport's clock.

## Network Simulator ##

The simulated network is vastly simpler than a real one, yet general enough to simulate many interesting systems. The protocol stack is flattened; there is only one kind of address, and it identifies a node. Nodes are configured with Java objects so that a variety of behaviors are possible and can be added to any simulation. There are two kernels available by default: A “router” kernel that implements RIP to route messages; and a “user” kernel for easily programming a client or server process against a simple API without dealing with the details of routing. These are intended for routers and leaf nodes, respectively, since most nodes on a real network are dedicated either to being network gateways or to client or server tasks. (Note that, since there is no separate link layer, router nodes also play the role of Ethernet hubs and switches in the real world.)

### API ###

We have a hierarchy of interfaces that represent
  * Nodes - A machine on the network. May be either a router or a leaf node.
  * Kernel - Low-level software running on the node; may host a Process or only handle routing.
  * Interface - A network interface connecting a node to another
  * Processes - User software loaded into a node; clients, servers, etc.
  * Operating System - The API for a Process to send and receive messages
  * Simulator - The main control interface to the simulation. From here, nodes are created, destroyed, and manipulated at will.

With that we have classes that represent
  * Interface Implementations - [KernelImpl](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/impl/kernel/KernelImpl.java), [NodeImpl](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/impl/NodeImpl.java), etc.
  * Protocols - [HTTP protocol](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/protocols/HTTP.java), [RIP protocol](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/protocols/RIP.java)
  * Software - [HTTPServer](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/software/HTTPServer.java)
  * Test Programs - [CountingTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/CountingTest.java), [CountingProcessTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/CountingProcessTest.java), [HTTPServerTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/HTTPServerTest.java), [KernelUserTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/KernelUserTest.java), etc.

### Operation ###

To run, a driver program obtains a Simulator implementation and builds several nodes. Each node has:

  * An integer address (can be auto-assigned)
  * A name (optional; for display purposes)
  * A kernel
    * If the kernel is a UserKernel, it may in turn contain a process
  * Other nodes to which it is connected

The kernel determines the behavior of the node; the only difference between a router and a leaf node is which kernel is loaded.

When the simulator is started, all nodes begin running in parallel. Messages are sent through the network using each
node's Interface objects; the Interface's send() and receive() methods operate on Message objects. A Message is simply a Serializable blob containing source and destination addresses and ports, along with some object with its
payload. When send() is called, the Message is serialized into a byte array and added to a BlockingQueue on the connected Interface, where it waits for a receive() call to remove it from the queue and deserialize it. (Serialization keeps us from cheating (at least easily) by sending across an object and manipulating it afterward, allowing for communication without going through the Interface.)

## Tests ##
  * [BullyTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/BullyTest.java)  - Test of network which implements the Bully Algorithm for establishing control.  The Bully Algorithm uses Lamport Clocks for the election process.
  * [TokenRingTwoPhaseTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/TokenRingTwoPhaseTest.java) - Test of network which implements the Ring Algorithm for establishing control.
  * [CountingTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/CountingTest.java) - Test two nodes connected to each other where one node sends a number the other one receives it, increments it and sends it back.
  * [CoutingProcessTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/CountingProcessTest.java) - Same as above but uses processes to count in token rings over a larger network. For a real stress test, one case randomly generates a network of 100 nodes (!).
  * [HTTPServerTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/HTTPServerTest.java) - Test of client-server http model
  * [KernelUserTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/KernelUserTest.java) - Test of simple network simulation of routers and computers
  * [MultiProcessTest](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/MultiProcessTest.java) - Tests if multiple processes can be running on the operating system.
  * [SimpleTestRun](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/SimpleTestRun.java) - Tests if the nodes are being created correctly.

## Other Notes ##
We are using TestNG to test are code. We have implemented Java Logger in all our classes to use for logging.

## Why our interface is useful to simulate a distributed system ##
Our network simulator is useful representation of real world networks and can be set-up in any configuration which physical networks exist.  The Nodes, which represent routers and computers, run independent of each other. Encapsulation of data and code forces the user to obey the normal physical constraints of contacting other nodes through "wired" connections rather than just accessing the information.

**[RIP Algorithm](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/impl/kernel/KernelImpl.java)**

> [KernelImpl](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/impl/kernel/KernelImpl.java) is an implementation of a Kernel that is designed to run a router. These routers send a message to all of their neighbors (directly connected nodes) requesting routing information. When routing information is received, we compare it against our table to see if we need to add to/update our table. Entries are only removed from our table when a message is unsuccessfully sent (we assume a dead connection somewhere along the way).

**[Bully Algorithm](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/BullyTest.java) on a simulated DS:**

> We tested a bully algorithm by simulating a network with five computers and two routers. Each computer has one process that is participating in the network by sending information (an almost empty message) to the elected leader. The leader then replies with an acknowledgment of receipt.

> If a process never receives an acknowledgment from its leader, it calls an election (by requesting votes from its superiors). If a vote is received, the process begins to wait for an announcement from the new leader (as there exists a superior to this process). If no superiors respond, the process declares itself the leader and sends the message to all participating processes.

> Each process has a logical election clock. This clock represents the most current election that the process knows about. When a process calls an election, it increments the clock and begins sending the messages. When an election related message is received, the election clock is checked to see if the message is relevant (i.e. not from an old election). The use of the election clocks enabled us to have multiple processes calling elections concurrently while still arriving at a single result. For example:

> When we start the test, the leader is a dead process, so all process call an election after incrementing their clocks to 1 (the clock starts at 0). So as far as each process is concerned, they started the election, but their action isn't any different than if they received a request for election. So the first election ends with a leader (except in the case that the leader hits the high end of a random sleep, in which case, the 'real' leader calls another election). So _b_ eventually becomes the leader (after one or two elections).

> _a_ then joins the network. _a_ calls an election and declares itself the winner (of election 1). _b_ (the current master, and winner of election 1) sees this, and calls a new election, in which _a_ is again declared winner (this time of election 2) and _b_ becomes a regular slave.

> After some time, _a_ dies, and a new election is called, to which _b_ wins. After another interval, a new process joins. This process calls an election (with election time 1), to which processes respond (and thus the clock is updated to the current election, #3). When _b_ receives the message to cast its vote, it also sees that it has already declared itself the winner to the current election, so it resends the victory message.

> Thus order is restored and the people rejoice.

**[Two Phase Commit](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/TokenRingTwoPhaseTest.java) on a simulated DS:**

> Nodes on a token ring are logically organized in a ring topology with data being transmitted sequentially from one node to the next with a control token circulating around the ring controlling access.  The logical topology is used for the two phase process of voting and commit/abort.

> In our example we have 5 nodes connected with two routers. We setup each node to have the address of the next node with the last node pointing back to the first node forming the ring. When we start the program the first node is set to be the one who wants to commit and they pass a token to the next node asking to commit. The next node will either accept it or reject it by putting its answer in the token and passing it along to the next node. After this is done for all nodes the node that asked to commit will get back the token and they can check it to see if anyone rejected the commit request. If everyone accepted or someone rejected the commit request the node will then send another token that will contain whether it aborted or it was a success. After that token comes back acknowledging everyone received the message of abort or success the node will then send a up for grabs token which says that now someone else can send a commit request by taking the up for grabs token and sending a commit request token and the we repeat the two phase token sending.

> Our test simulation allows the user to set the likelihood nodes will not agree to the commit request as well as the likelihood a node will want a token that is up for grabs.


## Project Contributions ##
  * Anthony: I mostly worked on the [KernelImpl](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/impl/kernel/KernelImpl.java) and the [Bully Test](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/BullyTest.java), with a lot of help from the team.
  * Daniel: I worked on initial project description of simulating a botnet with the rest of the team. Worked with Anthony and Kristy to develop the [KernelImpl](http://code.google.com/p/cis630networksim/source/browse/trunk/src/network/impl/kernel/KernelImpl.java) method that manages how routers perform the RIP algorithm and manage message routing. Created the [Two Phase Commit](http://code.google.com/p/cis630networksim/source/browse/trunk/src/test/TokenRingTwoPhaseTest.java) test case and worked with Anthony and Kristy to improve it.
  * Kristy: I helped with the initial project description and rhe initial design discussions.  I set up TestNG with an initial example test and wiki page.  I worked with the team on some implementation and debugging of various algorithms and implementations.  I helped organize and begin this documentation page.
  * Luke: I designed the basic outline of the API and wrote the implementations of the basic machinery (Simulator, Node, OperatingSystem, and UserKernel). Also wrote a simple HTTP server and client (not as interesting without the botnet simulation, though). Toward the end I helped troubleshoot KernelImpl, largely the RIP implementation, and I wrote the CountingTest and CountingProcessTest to exercise and test the simulation.