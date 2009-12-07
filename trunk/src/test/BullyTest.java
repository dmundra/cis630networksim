package test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import network.AbstractProcess;
import network.Message;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.OperatingSystem.DisconnectedException;

import org.testng.annotations.Test;

public class BullyTest extends AbstractTest {

    private static final long TIMEOUT = 0;
    private static final long WAIT_FOR_LEADER_RESPONSE = 10;
    private static final long WAIT_FOR_LEADER_RESPONSE_TO_ELECTION = WAIT_FOR_LEADER_RESPONSE * 2;
    private static final long WAIT_FOR_NEW_LEADER_RESPONSE_TO_ELECTION = WAIT_FOR_LEADER_RESPONSE_TO_ELECTION * 2;
    private static final Random rand = new Random();
    private volatile CountDownLatch goalSignal;

    private class BullyProcess extends AbstractProcess {

        private static final int WORK_PORT = 49;
        private static final int ELECTION_PORT = 50;
        private static final int RESULT_PORT = 51;
        private final ArrayList<Integer> members;
        private int leader;
        private int electionNum = 0;
        private int electionNumLastRequested = 0;
        private int elecitonNumLastReceived = 0;

        BullyProcess(ArrayList<Integer> members, int leader) {
            this.members = (ArrayList<Integer>) members.clone();
            this.leader = leader;
        }

        /**
         * sends a message to the leader. Returns true if we've received a
         * response
         *
         * @return true if the leader responds, false otherwise
         * @throws InterruptedException
         */
        private boolean sendToLeader() throws InterruptedException {

            // see if we are not the leader:
            if (os().address() != leader) {

                os().logger().log(Level.INFO, "Sending: {0}", leader);
                try {
                    os().send(leader, WORK_PORT, WORK_PORT, "are you there?");
                } catch (DisconnectedException e) {
                    os().logger().info("Oops. I've been disconnected.");
                }

                // now get the response:
                try {
                    Message<String> message = os().receive(WORK_PORT,
                            WAIT_FOR_LEADER_RESPONSE, TimeUnit.SECONDS).asType(
                            String.class);
                    os().logger().log(Level.INFO, "Received: {0}",
                            message.source);
                } catch (Exception e) {
                    // we didn't get a response, call an election:
                    return false;
                }
            } else {
                try {
                    while (true) {
                        Message<String> message = os().receive(WORK_PORT, 1,
                                TimeUnit.MILLISECONDS).asType(String.class);
                        os().logger().info("Hi! I am the leader");

                        try {
                            os().send(message.source, WORK_PORT, WORK_PORT,
                                    "Hi! I am the leader");
                        } catch (DisconnectedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    // We have nothing to do restart
                }
            }

            return true;

        }

        private BTuple sendElectionP1(Message<ETuple> electionMessage)
                throws InterruptedException {

            os().logger().info(
                    "Election called, start msg is null: " + (electionMessage == null));

            if (electionMessage == null) {
                // we must be starting an election, not responding to an
                // ongoing
                // election
                ++electionNum;

                // clear any old votes:
                // try {
                // while (true) {
                // Message<ETuple> msg = os().receive(ELECTION_PORT, 10,
                // TimeUnit.MILLISECONDS).asType(ETuple.class);
                //
                // if(msg == null){
                // break;
                // }else
                // }
                // } catch (Exception e) {
                // // do nothing, our buffer is ready
                // }
                //
                // // clear old results
                // try {
                // while (os().receive(RESULT_PORT, 10, TimeUnit.MILLISECONDS)
                // != null) {
                // }
                //
                // } catch (Exception e) {
                // // do nothing, our buffer is ready
                // }
            } else {
                // check to see if we need to update our election clock:
                if (electionMessage.data.electionNum >= electionNum) {
                    os().logger().info(
                            "my election num: " + electionNum + ", new election num: " + electionMessage.data.electionNum);
                    electionNum = electionMessage.data.electionNum;

                } else {
                    // this means that it's old:
                    os().logger().info(
                            "this is an old election, ignore election process");
                    return BTuple.get(false, false);
                }
            }

            if (electionNumLastRequested < electionNum) {
                electionNumLastRequested = electionNum;
                for (Integer i : members) {
                    if (i < os().address()) {
                        try {
                            os().send(i, ELECTION_PORT, ELECTION_PORT,
                                    ETuple.get(os().address(), electionNum));
                        } catch (DisconnectedException e) {
                            os().logger().info("Oops. I've been disconnected.");
                        }
                    }
                }
            }


            // see if anyone responds that is higher than us, respond to
            // lowers:
            boolean meLead = elecitonNumLastReceived < electionNum;

            // see if we are responding to an election, and what we should
            // do:
            if (electionMessage != null) {
                // we assume that they are lower than us as they responded
                // to us
                if (electionMessage.data.address < os().address()) {

                    // see if this is a newer election:
                    if (electionMessage.data.electionNum > electionNum) {
                        // this should never be:
                        throw new RuntimeException("error in our logic");
                    } else {
                        return BTuple.get(false, meLead);
                    }
                } else {

                    try {
                        os().send(electionMessage.source, ELECTION_PORT,
                                ELECTION_PORT,
                                ETuple.get(os().address(), electionNum));
                    } catch (DisconnectedException e) {
                        // not sure what to do here...
                    }
                }
            }

            try {
                while (meLead) {
                    Message<ETuple> message = os().receive(ELECTION_PORT,
                            WAIT_FOR_LEADER_RESPONSE_TO_ELECTION,
                            TimeUnit.SECONDS).asType(ETuple.class);

                    // see if this is a newer election:
                    if (message.data.electionNum > electionNum) {
                        // we have been working on an old election:
                        // let's go with the new
                        os().logger().info(
                                "we've been working on an old election " + message.data.address);
                        return sendElectionP1(message);
                    } else if (message.data.electionNum < electionNum) {
                        // we can ignore this message
                        os().logger().info(
                                "we got an old message, ignoring it " + message.data.address);
                    } else {
                        if (message.data.address > os().address()) {
                            // respond with the "I've voted" message
                            os().send(message.source, ELECTION_PORT,
                                    ELECTION_PORT,
                                    ETuple.get(os().address(), electionNum));

                            os().logger().info(
                                    "responding to a inferior chaps request to vote: " + message.data.address);
                        } else {
                            // we know that we aren't the leader, someone else
                            // is
                            meLead = false;

                            os().logger().info(
                                    "someone higher than I responded: " + message.data.address);
                        }
                    }

                }
            } catch (Exception e) {
            }

            return BTuple.get(true, meLead);

        }

        public boolean sendElectionP2(boolean meLead)
                throws InterruptedException {

            if (elecitonNumLastReceived < electionNum) {

                if (meLead) {
                    os().logger().info("I won! I am the leader - " + os().address());
                    leader = os().address();
                    // we win!
                    for (int j = 0; j < members.size(); ++j) {
                        Integer i = members.get(j);

                        try {
                            os().send(i, RESULT_PORT, RESULT_PORT,
                                    ETuple.get(os().address(), electionNum));
                        } catch (DisconnectedException e) {
                            os().logger().info("Oops. I've been disconnected.");
                        }

                    }
                } else {
                    // someone else should have won,
                    try {
                        Message<ETuple> message = os().receive(RESULT_PORT,
                                WAIT_FOR_NEW_LEADER_RESPONSE_TO_ELECTION,
                                TimeUnit.SECONDS).asType(ETuple.class);

                        os().logger().info(
                                "I am not the leader, but this guys says he is - " + message.data.address);

                        leader = message.data.address;
                        elecitonNumLastReceived = electionNum;

                    } catch (NullPointerException e) {
                        // we didn't get a response, call another election?
                        return false;
                    }
                }
            }

            return true;
        }

        protected void run() throws InterruptedException {

            Thread.sleep(8000);

            // remove ourselves from our list:
            for (int i = 0; i < members.size(); ++i) {
                if (members.get(i) == os().address()) {
                    members.remove(i);
                    break;
                }
            }

            while (true) {

                if (os().address() != leader) {
                    Thread.sleep(1000 * rand.nextInt(4));
                }

                boolean callElection = false;
                Message<ETuple> electionMessage = null;
                Message<ETuple> resultMessage = null;
                // see if an election is taking place or someone else has said
                // that
                // they won, in either case, we need to do an election:
                try {

                    // clear off our buffer until we see that we need an
                    // election:
                    while (!callElection) {

                        electionMessage = os().receive(ELECTION_PORT, 1,
                                TimeUnit.MILLISECONDS).asType(ETuple.class);

                        if (electionMessage.data.electionNum >= electionNum) {
                            electionNum = electionMessage.data.electionNum;
                            // call an election
                            callElection = true;

                            os().logger().info("(in run) need to respond to a message: " + electionMessage.data);
                        }
                    }
                } catch (NullPointerException e) {
                    // good, let's keep going
                }

                try {
                    //pop off all results
                    while (true) {
                        resultMessage = os().receive(RESULT_PORT, 1,
                                TimeUnit.MILLISECONDS).asType(ETuple.class);

                        // if (resultMessage.data.address > os().address()) {
                        // // call an election
                        // callElection = true;
                        // }

                        // see if it's a current election
                        if (resultMessage.data.electionNum >= electionNum) {
                            electionNum = resultMessage.data.electionNum;
                            // Call an election if we get a message from a
                            // inferior
                            // node
                            if (resultMessage.data.address < os().address()) {
                                // If its superior than us we check if it is
                                // superior than our leader
                                if (resultMessage.data.address < leader) {
                                    leader = resultMessage.data.address;
                                    //if we set call election on a previous loop, reset:
                                    callElection = false;

                                    os().logger().info(
                                            "(in run) I am not the leader, but this guys says he is - " + leader);
                                }
                            } else {
                                callElection = true;
                                os().logger().info("(in run) need to call new eleciton: " + resultMessage);
                            }
                        }
                    }
                } catch (NullPointerException e) {
                }

                if (callElection || !sendToLeader()) {
                    os().logger().info(
                            "Election code from run() electionMessage is null: " + (electionMessage == null));

                    while (true) {
                        BTuple ret = sendElectionP1(electionMessage);
                        if (ret.b1) {
                            if (sendElectionP2(ret.b2)) {
                                break;
                            } else {
                                os().logger().info("didn't make it out of p2");
                            }
                        } else {
                            break;
                        }
                    }
                }

            }
        }
    }

    @Test(description = "Test with 5 nodes connected by two routers", timeOut = TIMEOUT)
    public void test5Nodes() throws InterruptedException {

        final Simulator sim = SimulatorFactory.instance().createSimulator();

        final ArrayList<Integer> members = new ArrayList<Integer>();
        members.add(1);
        members.add(2);
        members.add(3);
        members.add(4);
        members.add(5);

        int initLeader = 0;

        final Node a = sim.buildNode(1).name("A").kernel(
                 sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node b = sim.buildNode(2).name("B").kernel(
                 sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node c = sim.buildNode(3).name("C").kernel(
                sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node d = sim.buildNode(4).name("D").kernel(
                 sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node e = sim.buildNode(5).name("E").kernel(
                 sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node router1 = sim.buildNode(6).name("Router1").connections(a, b, c).create();

        final Node router2 = sim.buildNode(7).name("Router2").connections(
                router1, d, e).create();

        sim.start();

        Thread.sleep(60000);
        sim.disconnect(router1.interfaces().get(0));

        Thread.sleep(Long.MAX_VALUE);

        sim.destroy();
    }

    public static class ETuple implements Serializable {

        int address;
        int electionNum;

        public static ETuple get(int address, int electionNum) {
            ETuple ret = new ETuple();
            ret.address = address;
            ret.electionNum = electionNum;
            return ret;
        }

        public String toString() {
            return "address:" + address + " electionNum:" + electionNum;
        }
    }

    public static class BTuple implements Serializable {

        boolean b1;
        boolean b2;

        public static BTuple get(boolean b1, boolean b2) {
            BTuple ret = new BTuple();
            ret.b1 = b1;
            ret.b2 = b2;
            return ret;
        }
    }
}
