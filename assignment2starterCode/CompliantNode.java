import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private double probabilityOfPairwiseConnectivity;
    private double probabilityOfMaliciousNode;
    private double probabilityOfDistributingTx;
    private int numerOfRounds;
    
    private boolean[] followees;
    private Set<Transaction> pendingTransactions;
    private boolean[] blacklisted;
    
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.probabilityOfPairwiseConnectivity = p_graph;
        this.probabilityOfMaliciousNode = p_malicious;
        this.probabilityOfDistributingTx = p_txDistribution;
        this.numerOfRounds = numRounds;
    }

    public void setFollowees(boolean[] followees) {
        this.blacklisted = new boolean[followees.length];
        this.followees = new boolean[followees.length];
        for (int i = 0; i < followees.length; i++)
            this.followees[i] = followees[i];
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> transactions = new HashSet<>(this.pendingTransactions);
        pendingTransactions.clear();
        return transactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        Set<Integer> senders = candidates.stream().map(c -> c.sender).collect(toSet());
        for (int i = 0; i < followees.length; i++)
            if (followees[i] && !senders.contains(i))
                this.blacklisted[i] = true;

        for (Candidate c : candidates)
            if (!this.blacklisted[c.sender])
                pendingTransactions.add(c.tx);
    }
}
