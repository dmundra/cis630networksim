package test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

public class Graph {
    private BitSet[] lowerAdjacencies;
    private int[] degrees;
    private int totalDegree;
    private int nodeCount;
    
    public Graph(BitSet[] adjacencies, int capacity) {
        final int nodeCount = adjacencies.length;
        this.lowerAdjacencies = new BitSet[capacity];
        this.degrees = new int[capacity];
        this.nodeCount = nodeCount;
        
        for (int ix = 0; ix < nodeCount; ix++) {
            final BitSet adjNodes = adjacencies[ix];
            this.totalDegree += (this.degrees[ix] = adjNodes.cardinality());
            this.lowerAdjacencies[ix] = (BitSet) adjNodes.clone();
            
            for (int node = adjNodes.nextSetBit(0); node != -1;
                    node = adjNodes.nextSetBit(node + 1)) {
                this.degrees[node]++;
                this.totalDegree++;
            }
        }
        
        assertCorrect();
    }
    
    public void addNode(BitSet adjacent) {
        if (nodeCount == lowerAdjacencies.length) {
            lowerAdjacencies = Arrays.copyOf(lowerAdjacencies, nodeCount * 2);
            degrees = Arrays.copyOf(degrees, nodeCount * 2);
        }
        
        lowerAdjacencies[nodeCount] = (BitSet) adjacent.clone();
        totalDegree += 2 * (degrees[nodeCount] = adjacent.cardinality());
        
        for (int node = adjacent.nextSetBit(0); node != -1;
                node = adjacent.nextSetBit(node + 1))
            degrees[node]++;
        
        nodeCount++;
        
        assertCorrect();
    }
    
    public void addNode(int ... adjacencies) {
        addNode(bits(adjacencies));
    }
    
    private static BitSet bits(int ... ixs) {
        final BitSet ans = new BitSet();
        for (int ix : ixs)
            ans.set(ix);
        return ans;
    }
    
    public void grow(Random random) {
        final BitSet adjNodes = new BitSet();
        boolean atLeastOnce = false;
        for (int ix = 0; ix < nodeCount; ix++) {
            if (random.nextInt(totalDegree) < (degrees[ix] + 1) / 2) {
                atLeastOnce = true;
                adjNodes.set(ix);
            }
        }
        
        if (!atLeastOnce)
            adjNodes.set(random.nextInt(nodeCount));
        
        addNode(adjNodes);
    }
    
    public void growTo(int size, Random random) {
        while (nodeCount < size)
            grow(random);
        
        assertCorrect();
    }
    
    public BitSet[] lowerAdjacencies() {
        final BitSet[] ans = new BitSet[nodeCount];
        for (int ix = 0; ix < nodeCount; ix++)
            ans[ix] = (BitSet) lowerAdjacencies[ix].clone();
        
        return ans;
    }
    
    public BitSet[] allAdjacencies() {
        final BitSet[] ans = new BitSet[nodeCount];
        
        for (int ix = 0; ix < nodeCount; ix++) {
            final BitSet adj = ans[ix] = (BitSet) lowerAdjacencies[ix].clone();
            for (int node = adj.nextSetBit(0); node != -1;
                    node = adj.nextSetBit(node + 1))
                ans[node].set(ix);
        }
        
        try {
            assert false;
        } catch (AssertionError e) {
            // Assertions are on; start looping
            for (int ix = 0; ix < nodeCount; ix++)
                assert degrees[ix] == ans[ix].cardinality();
        }
        
        return ans;
    }
    
    public int degree(int node) {
        return degrees[node];
    }
    
    private void assertCorrect() {
        // Quit early if assertions are off
        try {
            assert false;
            return;
        } catch (AssertionError e) { }
        
        int totalDegree = 0;
        int[] degrees = new int[nodeCount];
        for (int ix = 0; ix < nodeCount; ix++) {
            final BitSet adjNodes = lowerAdjacencies[ix];
            totalDegree += (degrees[ix] = adjNodes.cardinality());
            
            for (int node = adjNodes.nextSetBit(0); node != -1;
                    node = adjNodes.nextSetBit(node + 1)) {
                assert node < ix : "Adjacency too large: " + node + " >= " + ix;
                
                degrees[node]++;
                totalDegree++;
            }
        }
        
        for (int ix = 0; ix < nodeCount; ix++)
            assert degrees[ix] == this.degrees[ix] :
                String.format("Wrong degree at %d: stored %d, calculated %d",
                        ix, this.degrees[ix], degrees[ix]);
        
        assert totalDegree == this.totalDegree :
            "Wrong total degree: stored " + this.totalDegree +
            ", calculated " + totalDegree; 
    }
}
