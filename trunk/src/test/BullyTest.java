package test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import network.AbstractProcess;
import network.Message;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.OperatingSystem.DisconnectedException;

import network.UserKernel;
import org.testng.annotations.Test;

public class BullyTest extends AbstractTest {

    private static final long TIMEOUT = 0;
    private static final long WAIT_FOR_LEADER_RESPONSE = 1000;
    private static final long WAIT_FOR_LEADER_RESPONSE_TO_ELECTION = WAIT_FOR_LEADER_RESPONSE * 2;
    private static final long WAIT_FOR_NEW_LEADER_RESPONSE_TO_ELECTION = WAIT_FOR_LEADER_RESPONSE_TO_ELECTION * 2;
    private static final Random rand = new Random();

    private class BullyProcess extends AbstractProcess {

        private static final int WORK_PORT = 49;
        private static final int ELECTION_PORT = 50;
        private static final int RESULT_PORT = 51;
        private final ArrayList<Integer> members;
        private int leader;
        private int electionNum = 0;
        private int electionNumLastRequested = 0;
        private int elecitonNumLastReceived = 0;
        private boolean iShouldBeLeader = true;

        @SuppressWarnings("unchecked")
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

                //os().logger().log(Level.INFO, "Sending info to my leader: {0}", leader);

                try {
                    os().send(leader, WORK_PORT, WORK_PORT, "are you there?");
                } catch (DisconnectedException e) {
                    os().logger().info("Oops. I've been disconnected.");
                }

                // now get the response:
                try {
                    Message<String> message = os().receive(WORK_PORT,
                            WAIT_FOR_LEADER_RESPONSE, TimeUnit.MILLISECONDS).asType(
                            String.class);
                    os().logger().log(Level.INFO, "Got info back from my leader: {0}", message.source);
                } catch (Exception e) {
                    // we didn't get a response, call an election:
                    return false;
                }
            } else {
                try {
                    while (true) {
                        Message<String> message = os().receive(WORK_PORT, 1,
                                TimeUnit.MILLISECONDS).asType(String.class);

//						os().logger().info(
//						        "I am the leader, responding to "
//						                + message.source);

                        try {
                            os().send(message.source, WORK_PORT, WORK_PORT, "Hi! I am the leader");
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

        private void sendElectionP1(Message<ETuple> electionMessage)
                throws InterruptedException {

            if (electionMessage.source == os().address()) {
                //this was one we sent to a dead node:
                removeMemberIfNeeded(os().address());
                //we are done:
                return;
            }


            // make sure we have this guy in our members:
            addMemberIfNeeded(electionMessage.source);

            // check to see if we need to update our election clock:
            if (electionMessage.data.electionNum > electionNum) {
                os().logger().info(
                        "my election num: " + electionNum + ", new election num: " + electionMessage.data.electionNum);

                setElectionNum(electionMessage.data.electionNum);
            }

            // propagate election to superiors (but only once per election)
            callElection(false);

            if (electionMessage.data.address < os().address()) {
                // we assume that they are responding to us

                if (electionMessage.data.electionNum == electionNum) {
                    // this is a current election, they responded, so we
                    // can't be the leader:
                    iShouldBeLeader = false;
                }
            } else {
                // we are their superior, respond (regardless of the
                // election number)
                try {



                    os().send(electionMessage.source, ELECTION_PORT,
                            ELECTION_PORT,
                            ETuple.get(os().address(), electionNum));
                } catch (DisconnectedException e) {
                    os().logger().info("Oops. I've been disconnected.");
                }
            }


        }

        private void callElection(boolean newElection)
                throws InterruptedException {


            if (newElection) {
                setElectionNum(electionNum + 1);
                leader = 0;
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
            } else if (newElection) {
                throw new RuntimeException("error in our logic, we are calling a new election when the last requested election number is not less than the election number");
            }

            if (newElection) {
                // give people some time to respond:
                Thread.sleep(WAIT_FOR_LEADER_RESPONSE_TO_ELECTION);
            }

        }

        public boolean electionP2() throws InterruptedException {


            if (elecitonNumLastReceived < electionNum) {

                if (iShouldBeLeader) {
                    os().logger().info(
                            "I won! I am the leader - " + os().address());
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

                    // don't send out again:
                    elecitonNumLastReceived = electionNum;
                } else {
                    // someone else should have won,
                    try {

                        while (true) {
                            Message<ETuple> message = os().receive(RESULT_PORT,
                                    WAIT_FOR_NEW_LEADER_RESPONSE_TO_ELECTION,
                                    TimeUnit.MILLISECONDS).asType(ETuple.class);

                            if (message.data.electionNum < electionNum) {
                                //skip this old message
                                continue;
                            } else if (message.data.electionNum == electionNum) {

                                os().logger().info(
                                        "I am not the leader, but this guys says he is - " + message.data.address);

                                leader = message.data.address;
                                elecitonNumLastReceived = electionNum;

                                break;
                            } else {
                                throw new RuntimeException("error in our logic: we say it's election #" + electionNum + ", but got a " + message.data.electionNum);
                            }
                        }
                    } catch (NullPointerException e) {
                        // we didn't get a response, call another election?
                        os().logger().info(
                                "no buddy said they were the leader...");

                        return false;
                    }
                }
            }else{
                //see if we have any new election messages
                try{
                Message<ETuple> message = os().receive(RESULT_PORT,
                                    1,
                                    TimeUnit.MILLISECONDS).asType(ETuple.class);

                if (message.data.electionNum >= electionNum) {
                    //something went wrong, call a new election:
                    if(message.data.electionNum > electionNum){
                        setElectionNum(message.data.electionNum);
                    }
                    callElection(false);
                }

                }catch(NullPointerException e){
                    //nothing to see, move along
                }
            }

            return true;
        }

        /**
         * this adds a member to our list if not already present
         *
         * @param member
         *            the new member to add
         * @return true if we add, false otherwise
         */
        private boolean addMemberIfNeeded(int member) {


            if (member == os().address()) {
                return false;
            }


            for (int i : members) {
                if (i == member) {
                    return false;
                }
            }

            members.add(member);
            return true;


        }

        private boolean removeMemberIfNeeded(int member) {


            for (int i = 0; i < members.size(); ++i) {
                if (members.get(i) == member) {
                    members.remove(i);
                    return true;
                }
            }


            return false;


        }

        private void electionP1() throws InterruptedException {
            electionP1(0);
        }

        private void electionP1(long timeOut) throws InterruptedException {


            try {
                // pop off all the messages until we get to one that
                // requires
                // work:


                while (true) {


                    Message<ETuple> message = os().receive(ELECTION_PORT,
                            timeOut, TimeUnit.MILLISECONDS).asType(ETuple.class);

                    // this will set our iShouldBeLeader
                    sendElectionP1(message);

                    timeOut = WAIT_FOR_LEADER_RESPONSE_TO_ELECTION;
                }
            } catch (Exception e) {
                // ok we don't have any more messages
//                                   
            }

            if (!electionP2()) {
                // we didn't make it, call a new election:

                callElection(true);
                electionP1(WAIT_FOR_LEADER_RESPONSE_TO_ELECTION);
                //break;
            } else {
                // good, we are done:
                //break;
            }


        }

        private void setElectionNum(int electionNum) {

            if (electionNum <= this.electionNum) {
                throw new RuntimeException("error in our logic, we are trying to set our election to something less than or equal to the current");
            }

            this.electionNum = electionNum;
            iShouldBeLeader = true;

        }

        protected void run() throws InterruptedException {

            Thread.sleep(2000);

            // remove ourselves from our list:
            for (int i = 0; i < members.size(); ++i) {
                if (members.get(i) == os().address()) {
                    members.remove(i);
                    break;
                }
            }

            long p1Wait = 0;

            while (true) {



                if (os().address() != leader) {
                    Thread.sleep(1000 * rand.nextInt(2) + 1000);
                }

                // checks our election port for any work
                electionP1(p1Wait);
                p1Wait = 0;



                // checks our results port for any work
                electionP2();


                if (!sendToLeader()) {




                    // call a new election
                    callElection(true);
                    p1Wait = WAIT_FOR_LEADER_RESPONSE_TO_ELECTION;
                }


            }
        }
    }

    //for non testNG
    public static void main(String[] args) throws InterruptedException {
        new BullyTest().test5Nodes();
    }

    @Test(description = "Test with 5 nodes connected by two routers", timeOut = TIMEOUT)
    public void test5Nodes() throws InterruptedException {

        final Simulator sim = SimulatorFactory.instance().createSimulator();

        final ArrayList<Integer> members = new ArrayList<Integer>();
        int startIndex = 2;
        members.add(startIndex + 1);
        members.add(startIndex + 2);
        members.add(startIndex + 3);
        members.add(startIndex + 4);
        members.add(startIndex + 5);

        int initLeader = 0;

        final Node a = sim.buildNode(startIndex + 1).name("A").kernel(
                sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node b = sim.buildNode(startIndex + 2).name("B").kernel(
                sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node c = sim.buildNode(startIndex + 3).name("C").kernel(
                sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node d = sim.buildNode(startIndex + 4).name("D").kernel(
                sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node e = sim.buildNode(startIndex + 5).name("E").kernel(
                sim.createUserKernel(new BullyProcess(members, initLeader))).create();

        final Node router1 = sim.buildNode(startIndex + 6).name("Router1").connections(a, b,
                c).create();
        @SuppressWarnings("unused")
        final Node router2 = sim.buildNode(startIndex + 7).name("Router2").connections(
                router1, d, e).create();

        sim.start();

        Thread.sleep(20000);
        sim.disconnect(router1.interfaces().get(0));

        Thread.sleep(15000);
        @SuppressWarnings("unused")

        final Node f = sim.buildNode().name("F").connections(router1).kernel(sim.createUserKernel(new BullyProcess(members, 0))).create();
        

        //now add A back in:
//        Thread.sleep(15000);
//
//        final Node router3 = sim.buildNode(startIndex + 9).name("Router3").connections(a, router2).create();
//        ((UserKernel)a.kernel()).setProcess(new BullyProcess(members, initLeader));



        Thread.sleep(Long.MAX_VALUE);

        sim.destroy();
    }

    public static class ETuple implements Serializable {

        private static final long serialVersionUID = -31652L;
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

        private static final long serialVersionUID = 931628L;
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
