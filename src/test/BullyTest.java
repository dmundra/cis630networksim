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

/**
 * this is a test for running a bully algorith over a few nodes
 */
public class BullyTest extends AbstractTest {

    private static final long TIMEOUT = 0;
    private static final long WAIT_FOR_LEADER_RESPONSE = 1000;
    private static final long WAIT_FOR_LEADER_RESPONSE_TO_ELECTION = WAIT_FOR_LEADER_RESPONSE * 2;
    private static final long WAIT_FOR_NEW_LEADER_RESPONSE_TO_ELECTION = WAIT_FOR_LEADER_RESPONSE_TO_ELECTION * 2;
    private static final Random rand = new Random();

    /**
     * this is the process that all of the operating systems will run
     */
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

        /**
         * takes in the list of current members and the current leader
         * @param members the list of members
         * @param leader the current leader
         */
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

                os().logger().log(Level.INFO, "Sending info to my leader: {0}", leader);

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

                        os().logger().info(
                                "I am the leader, responding to " + message.source);

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

        /**
         * calls an election by sending a request to vote to all superiors
         * @param newElection if true, we'll increment our election clock and
         *        send our messages. If false, we'll send our messages only if
         *        we've never done so (for this election)
         * @throws InterruptedException
         */
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

        /**
         * pops off all of our election messages (dealing with them) and starts
         * phase 2.
         * @param timeOut initial time out we should use when popping messages
         * @throws InterruptedException
         */
        private void electionP1(long timeOut) throws InterruptedException {


            boolean needToResendWinningMessage = false;
            try {
                // pop off all the messages until we get to one that
                // requires
                // work:
                while (true) {
                    Message<ETuple> message = os().receive(ELECTION_PORT,
                            timeOut, TimeUnit.MILLISECONDS).asType(ETuple.class);

                    // this will set our iShouldBeLeader
                    if (sendElectionP1(message)) {
                        needToResendWinningMessage = true;
                    }

                    timeOut = WAIT_FOR_LEADER_RESPONSE_TO_ELECTION;
                }
            } catch (Exception e) {
                // ok we don't have any more messages, move on
            }
            if (!electionP2(needToResendWinningMessage)) {
                // we didn't make it, call a new election:

                callElection(true);
                electionP1(WAIT_FOR_LEADER_RESPONSE_TO_ELECTION);
            }


        }

        /**
         * this handles sending response messages from our phase 1 (voting)
         * @param electionMessage the message we've received
         * @return true if we think that a victory message should be sent (if we are the winner)
         * @throws InterruptedException
         */
        private boolean sendElectionP1(Message<ETuple> electionMessage)
                throws InterruptedException {

            if (electionMessage.source == os().address()) {
                //this was one we sent to a dead node:
                removeMemberIfNeeded(os().address());
                //we are done:
                return false;
            }


            // make sure we have this guy in our members:
            addMemberIfNeeded(electionMessage.source);

            // check to see if we need to update our election clock:
            if (electionMessage.data.electionNum > electionNum) {
                os().logger().info(
                        "updating my election num: " + electionNum + ", new election num: " + electionMessage.data.electionNum);

                setElectionNum(electionMessage.data.electionNum);
            }

            // propagate election to superiors (but only once per election)
            callElection(false);

            if (electionMessage.data.address < os().address()) {
                // we assume that they are responding to a message we sent

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

                    os().logger().info(
                            "responding to an inferior - " + electionMessage.source);

                    //since they may be late bloomers, we'll tell them we are
                    //the leader (if we are)
                    return true;

                } catch (DisconnectedException e) {
                    os().logger().info("Oops. I've been disconnected.");
                }
            }

            return false;


        }

        /**
         * handles our phase 2 (sending victory or receiving victory message)
         * @return false if we were expecting a victory message, but never got it,
         *         true otherwise.
         * @throws InterruptedException
         */
        public boolean electionP2() throws InterruptedException {
            return electionP2(false);
        }

        /**
         * handles our phase 2 (sending victory or receiving victory message)
         * @param resendWinningMessage if true, we'll resend the victory message
         *        if we were the winner from the current election
         * @return false if we were expecting a victory message, but never got it,
         *         true otherwise.
         * @throws InterruptedException
         */
        public boolean electionP2(boolean resendWinningMessage) throws InterruptedException {


            if (resendWinningMessage && elecitonNumLastReceived == electionNum &&
                    iShouldBeLeader && leader == os().address()) {

                //we need to resend our victory (this will be reupdated in the next block):
                --elecitonNumLastReceived;
            }

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

                    // don't send out again (unless we really need to):
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
            } else {
                //see if we have any new election messages
                try {
                    Message<ETuple> message = os().receive(RESULT_PORT,
                            1,
                            TimeUnit.MILLISECONDS).asType(ETuple.class);

                    if (message.data.electionNum >= electionNum) {

                        
                        if (message.data.address > os().address()) {
                            //this is from an inferior:
                            if (message.data.electionNum > electionNum) {
                                setElectionNum(message.data.electionNum);

//                            } else if (message.data.electionNum == electionNum &&
//                                    elecitonNumLastReceived == electionNum &&
//                                    iShouldBeLeader && leader == os().address()) {
//                                //make sure to call a new election as we thought that
//                                //we were the winner on this election:
//                                newNewElection = true;
                            }

                            callElection(true);
                        }


                        //something went wrong, call a new election:

                    }

                } catch (NullPointerException e) {
                    //nothing to see, move along
                }
            }

            return true;
        }

        /**
         * adds a member to our list if not already present
         * @param member the new member to add
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

        /**
         * removes a member from our list
         * @param member the member to remove
         * @return true if we remove, false otherwise
         */
        private boolean removeMemberIfNeeded(int member) {


            for (int i = 0; i < members.size(); ++i) {
                if (members.get(i) == member) {
                    members.remove(i);
                    return true;
                }
            }


            return false;


        }

        /**
         * this sets our election number to the given input if it is less than
         * or equal to our current election number. It also resets our variable
         * that helps determine if we are the winner of this current election
         * @param electionNum the new election number
         */
        private void setElectionNum(int electionNum) {

            if (electionNum <= this.electionNum) {
                throw new RuntimeException("error in our logic, we are trying to set our election to something less than or equal to the current");
            }

            this.electionNum = electionNum;
            iShouldBeLeader = true;

        }

        /**
         * this is the method that the OS will call on us
         * @throws InterruptedException
         */
        protected void run() throws InterruptedException {

            //let routers get set up
            Thread.sleep(2000);

            // remove ourselves from our list:
            for (int i = 0; i < members.size(); ++i) {
                if (members.get(i) == os().address()) {
                    members.remove(i);
                    break;
                }
            }

            //initial wait for looking for election messages
            long p1Wait = 0;

            //loop forever
            while (true) {

                //if we aren't the leader, let's sleep for a time
                if (os().address() != leader) {
                    Thread.sleep(1000 * rand.nextInt(2) + 1000);
                }

                //checks our election port for any work, moves us on to phase 2
                //if needed
                electionP1(p1Wait);
                //resets our wait time (in case it has been changed on the last
                //iteration
                p1Wait = 0;


                //communicate with our leader
                if (!sendToLeader()) {
                    //our leader didn't respond!
                    //call a new election
                    callElection(true);
                    //for next iteration:
                    p1Wait = WAIT_FOR_LEADER_RESPONSE_TO_ELECTION;
                }
            }
        }
    }

    //for non testNG
    public static void main(String[] args) throws InterruptedException {
        new BullyTest().test5Nodes();
    }

    /**
     * this test starts out with 5 nodes and two routers. We kill off the leader
     * and add in a new node to observe the behavior
     * @throws InterruptedException
     */
    @Test(description = "Test with 5 nodes connected by two routers", timeOut = TIMEOUT)
    public void test5Nodes() throws InterruptedException {

        final Simulator sim = SimulatorFactory.instance().createSimulator();

        final ArrayList<Integer> members = new ArrayList<Integer>();
        int startIndex = 0;
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
