import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class MaxFeeTxHandler {
    private UTXOPool utxoPool;
    
    public MaxFeeTxHandler(UTXOPool utxoPool) {
       this.utxoPool = new UTXOPool(utxoPool);
    }
    
    private double transactionFee(Transaction transaction) {        
        double inputValueSum = 0;
        for (Transaction.Input input : transaction.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!utxoPool.contains(utxo) || !isValidTx(transaction)) continue;
            Transaction.Output txOutput = utxoPool.getTxOutput(utxo);
            inputValueSum += txOutput.value;
        }
        
        double outputValueSum = 0;
        for (Transaction.Output output : transaction.getOutputs())
            outputValueSum += output.value;
        
        return inputValueSum - outputValueSum;
    }
    
    public boolean isValidTx(Transaction tx) {
        UTXOPool uniqueUTXOPool = new UTXOPool();
        double outputValueSum = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) // (4)
                return false;
            outputValueSum += output.value;
        }
        
        double inputValueSum = 0;
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            
            if (!utxoPool.contains(utxo)) // (1)
                return false;
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) // (2)
                return false;
            if (uniqueUTXOPool.contains(utxo)) // (3)
                return false;
            if (output.value < 0) // (4)
                return false;
                      
            inputValueSum += output.value;
            uniqueUTXOPool.addUTXO(utxo, output);
        }
                
        return outputValueSum <= inputValueSum; // (5)      
    }

    
    public Transaction[] handleTxs(Transaction[] transactions) {        
        Set<Transaction> sortedTransactions = new TreeSet<>((t1, t2) -> {
            double t1Fee = transactionFee(t1);
            double t2Fee = transactionFee(t2);
            return Double.valueOf(t1Fee).compareTo(t2Fee);
        });
        Collections.addAll(sortedTransactions, transactions);
        
        Set<Transaction> validTxs = new HashSet<>();        
        for (Transaction tx : sortedTransactions) {
            if (isValidTx(tx)) {
                validTxs.add(tx);
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output out = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, out);
                }
            }
        }
        
        Transaction[] validTxArray = new Transaction[validTxs.size()];
        return validTxs.toArray(validTxArray);
    }
}
