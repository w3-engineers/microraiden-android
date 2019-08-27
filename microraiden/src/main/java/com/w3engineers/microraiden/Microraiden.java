package com.w3engineers.microraiden;

import com.w3engineers.microraiden.contracts.RaidenMicroTransferChannels;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.lang.ref.SoftReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jnr.ffi.annotations.In;

public class Microraiden {
    private Executor executor;
    private ExecutorService callableExecutor;
    private ContractGasProvider contractGasProvider;
    private Web3j web3j;
    private static String contractAddress = null;
    private MicroraidenListener listener;

    public Microraiden(String contractAddress, String rpcUrl, long gasPrice, long gasLimit, MicroraidenListener microraidenListener) {

        this.executor = Executors.newSingleThreadExecutor();
        this.callableExecutor = Executors.newFixedThreadPool(1);
        this.contractAddress = contractAddress;
        this.listener = microraidenListener;


        this.web3j = Web3j.build(new HttpService(rpcUrl));

        this.contractGasProvider = new ContractGasProvider() {
            @Override
            public BigInteger getGasPrice(String contractFunc) {
                return BigInteger.valueOf(gasPrice);
            }

            @Override
            public BigInteger getGasPrice() {
                return BigInteger.valueOf(gasPrice);
            }

            @Override
            public BigInteger getGasLimit(String contractFunc) {
                return BigInteger.valueOf(gasLimit);
            }

            @Override
            public BigInteger getGasLimit() {
                return BigInteger.valueOf(gasLimit);
            }
        };
    }

    private RaidenMicroTransferChannels loadChannelManager(Credentials credentials) {
        return RaidenMicroTransferChannels.load(contractAddress, web3j, credentials, contractGasProvider);
    }

    private double getETHorTOKEN(BigInteger value) {
        BigDecimal tokenValue = Convert.fromWei(new BigDecimal(value), Convert.Unit.ETHER);
        return tokenValue.doubleValue();
    }

    public BigInteger getWeiValue(double value) {
        BigDecimal weiTokenValue = Convert.toWei(BigDecimal.valueOf(value), Convert.Unit.ETHER);
        BigInteger b = weiTokenValue.toBigInteger();
        return b;
    }

    public Tuple5<byte[], Double, Long, Double, Double> getChannelInfo(String sender, String receiver, long openBlock,  Credentials credentials) throws ExecutionException, InterruptedException {

        Future<Tuple5<byte[], Double, Long, Double, Double>> future = callableExecutor.submit(new Callable<Tuple5<byte[], Double, Long, Double, Double>>() {
            @Override
            public Tuple5<byte[], Double, Long, Double, Double> call() throws Exception {

                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);

                Tuple5<byte[], BigInteger, BigInteger, BigInteger, BigInteger> values = null;
                Tuple5<byte[], Double, Long, Double, Double> output = null;

                try {
                    values = channelManager.getChannelInfo(sender, receiver, BigInteger.valueOf(openBlock)).send();
                    if (values != null) {
                        byte[] chKey = values.getValue1();
                        double deposit = getETHorTOKEN(values.getValue2());
                        long settleBlockNumber = values.getValue3().longValue();
                        double closingBalance = getETHorTOKEN(values.getValue4());
                        double withdraw = getETHorTOKEN(values.getValue5());

                        output = new Tuple5<>(chKey, deposit, settleBlockNumber, closingBalance, withdraw);
                        return output;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return output;
            }
        });
        return future.get();
    }

    public String getBalanceProof(String receiver, long openBlock, double balance, Credentials credentials) {
        List<String> labels = Arrays.asList(
                "string message_id",
                "address receiver",
                "uint32 block_created",
                "uint192 balance",
                "address contract"
        );

        int l = 0;
        for (String item : labels) {
            l += item.getBytes().length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(l);

        for (String a : labels) {
            buffer.put(a.getBytes());
        }
        byte[] array = buffer.array();

        List<byte[]> values = Arrays.asList(
                "Sender balance proof signature".getBytes(),
                Numeric.hexStringToByteArray(receiver),
                Numeric.toBytesPadded(BigInteger.valueOf(openBlock), 4),
                Numeric.toBytesPadded(getWeiValue(balance), 24),
                Numeric.hexStringToByteArray(contractAddress)
        );

        int v = 0;
        for (byte[] item : values) {
            v += item.length;
        }
        ByteBuffer bufferValues = ByteBuffer.allocate(v);

        for (byte[] a : values) {
            bufferValues.put(a);
        }
        byte[] arrayValues = bufferValues.array();

        ByteBuffer byteArrayBuffer = ByteBuffer.allocate(64);
        byteArrayBuffer.put(Hash.sha3(array));
        byteArrayBuffer.put(Hash.sha3(arrayValues));
        byte[] arrayValuesBuffer = byteArrayBuffer.array();

        Sign.SignatureData signature = Sign.signMessage(arrayValuesBuffer, credentials.getEcKeyPair());

        //Balance Proof Signature
        ByteBuffer sigBuffer = ByteBuffer.allocate(signature.getR().length + signature.getS().length + 1);
        sigBuffer.put(signature.getR());
        sigBuffer.put(signature.getS());
        sigBuffer.put(signature.getV());
        String balanceProofSignature = Numeric.toHexString(sigBuffer.array());

        return balanceProofSignature;
    }

    public String verifyBalanceProofSignature(String receiver, long openBlock, double balance, String balanceProof, Credentials credentials) throws ExecutionException, InterruptedException {

        Future<String> future = callableExecutor.submit(new Callable() {
            @Override
            public String call() throws Exception {
                try {
                    RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                    String sender = channelManager.extractBalanceProofSignature(receiver, BigInteger.valueOf(openBlock), getWeiValue(balance), Numeric.hexStringToByteArray(balanceProof)).send();
                    return sender;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }

    public String getClosingHash(String sender, long openBlock, double balance, Credentials credentials) {
        List<String> labels = Arrays.asList(
                "string message_id",
                "address sender",
                "uint32 block_created",
                "uint192 balance",
                "address contract"
        );

        int l = 0;
        for (String item : labels) {
            l += item.getBytes().length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(l);

        for (String a : labels) {
            buffer.put(a.getBytes());
        }
        byte[] array = buffer.array();

        List<byte[]> values = Arrays.asList(
                "Receiver closing signature".getBytes(),
                Numeric.hexStringToByteArray(sender),
                Numeric.toBytesPadded(BigInteger.valueOf(openBlock), 4),
                Numeric.toBytesPadded(getWeiValue(balance), 24),
                Numeric.hexStringToByteArray(contractAddress)
        );

        int v = 0;
        for (byte[] item : values) {
            v += item.length;
        }
        ByteBuffer bufferValues = ByteBuffer.allocate(v);

        for (byte[] a : values) {
            bufferValues.put(a);
        }
        byte[] arrayValues = bufferValues.array();

        ByteBuffer byteArrayBuffer = ByteBuffer.allocate(64);
        byteArrayBuffer.put(Hash.sha3(array));
        byteArrayBuffer.put(Hash.sha3(arrayValues));
        byte[] arrayValuesBuffer = byteArrayBuffer.array();

        Sign.SignatureData signature = Sign.signMessage(arrayValuesBuffer, credentials.getEcKeyPair());

        //Closing Signature
        ByteBuffer sigBuffer = ByteBuffer.allocate(signature.getR().length + signature.getS().length + 1);
        sigBuffer.put(signature.getR());
        sigBuffer.put(signature.getS());
        sigBuffer.put(signature.getV());
        String closingHashSignature = Numeric.toHexString(sigBuffer.array());

        return closingHashSignature;
    }

    public String verifyClosingHashSignature(String sender, long openBlock, double balance, String closingSig, Credentials credentials) throws ExecutionException, InterruptedException {
        Future<String> future = callableExecutor.submit(new Callable() {
            @Override
            public String call() throws Exception {
                try {
                    RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                    String receiver = channelManager.extractClosingSignature(sender, BigInteger.valueOf(openBlock), getWeiValue(balance), Numeric.hexStringToByteArray(closingSig)).send();
                    return receiver;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }

    public Integer getChallengePeriod(Credentials credentials) throws ExecutionException, InterruptedException {
        Future<Integer> future = callableExecutor.submit(new Callable() {
            @Override
            public Integer call() throws Exception {
                try {
                    RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                    Integer challenge_period = channelManager.challenge_period().send().intValue();
                    return challenge_period;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }

    public String getVersion(Credentials credentials) throws ExecutionException, InterruptedException {
        Future<String> future = callableExecutor.submit(new Callable() {
            @Override
            public String call() throws Exception {
                try {
                    RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                    String version = channelManager.version().send();
                    return version;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }

    public byte[] getChannelKey(String senderAddress, String receiverAddress, long openBlock, Credentials credentials) throws ExecutionException, InterruptedException {
        Future<byte[]> future = callableExecutor.submit(new Callable() {
            @Override
            public byte[] call() throws Exception {
                try {
                    RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                    byte[] key = channelManager.getKey(senderAddress, receiverAddress, BigInteger.valueOf(openBlock)).send();
                    return key;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }

    public String getOwnerAddress(Credentials credentials) throws ExecutionException, InterruptedException {
        Future<String> future = callableExecutor.submit(new Callable() {
            @Override
            public String call() throws Exception {
                try {
                    RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                    String address = channelManager.owner_address().send();
                    return address;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }


    public String getTokenAddress(Credentials credentials) throws ExecutionException, InterruptedException {
        Future<String> future = callableExecutor.submit(new Callable() {
            @Override
            public String call() throws Exception {
                try {
                    RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                    String address = channelManager.token().send();
                    return address;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }


    public Double getChannelDepositLimit(Credentials credentials) throws ExecutionException, InterruptedException {
        Future<Double> future = callableExecutor.submit(new Callable() {
            @Override
            public Double call() throws Exception {

                try {
                    RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                    BigInteger limit = channelManager.channel_deposit_bugbounty_limit().send();
                    return getETHorTOKEN(limit);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }


    public interface MicroraidenListener {
        void onBalanceWithDrawn(String txStatus, String txHash);
        void onWithDrawnError(String msg);

        void onChannelCreated(String txStatus, String txHash, long openBlock);
        void onChannelCreateError(String msg);

        void onChannelToppedUp(String txStatus, String txHash);
        void onChannelTopupError(String msg);

        void onChannelClosed(String txStaus, String txHash);
        void onChannelCloseError(String msg);

        void onTrustedContractsAdded(String txStatus, String txHash);
        void onTrustedContractAddError(String msg);

        void onTrustedContractsRemoved(String txStatus, String txHash);
        void onTrustedContractRemoveError(String msg);

        void onChannelClosedUncooperative(String txStatus, String txHash);
        void onChannelCloseUncooperativeError(String msg);

        void onChannelSettled(String txStatus, String txHash);
        void onChannelSettleError(String msg);

        void onToppedUpDelegate(String txStatus, String txHash);
        void onTopUpDelegateError(String msg);

        void onChannelCreatedDelegate(String txStatus, String txHash);
        void onChannelCreateDelegateError(String msg);


    }

    public void withdrawBalance(final long block_number, final double balance, final String balance_signature, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    BigInteger wieValue = getWeiValue(balance);
                    TransactionReceipt txRcp = channelManager.withdraw(BigInteger.valueOf(block_number), wieValue, Numeric.hexStringToByteArray(balance_signature)).send();
                    if (txRcp != null) {
                        listener.onBalanceWithDrawn(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onWithDrawnError("Unable to get balance withdrawn status!");
                    }
                } catch (Exception e) {
                    listener.onWithDrawnError(e.getMessage());
                }
            }
        });
    }

    public void createChannel(final String receiver, final double deposit, Credentials credentials) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.createChannel(receiver, getWeiValue(deposit)).send();

                    if (txRcp != null) {
                        listener.onChannelCreated(txRcp.getStatus(), txRcp.getTransactionHash(), txRcp.getBlockNumber().longValue());
                    } else {
                        listener.onChannelCreateError("Unable to get channel create status!");
                    }
                } catch (Exception e) {
                    listener.onChannelCreateError(e.getMessage());
                }
            }
        });
    }

    public void topUpChannel(final String receiver, final long blockNumber, final double added_deposit, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.topUp(receiver, BigInteger.valueOf(blockNumber), getWeiValue(added_deposit)).send();
                    if (txRcp != null) {
                        listener.onChannelToppedUp(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onChannelTopupError("Unable to get channel topup status!");
                    }
                } catch (Exception e) {
                    listener.onChannelTopupError(e.getMessage());
                }
            }
        });
    }

    public void closeChannelCooperative(final String receiver, final long openBlock, final double balance, final String balance_signature, final String closing_signature, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.cooperativeClose(receiver, BigInteger.valueOf(openBlock), getWeiValue(balance), Numeric.hexStringToByteArray(balance_signature), Numeric.hexStringToByteArray(closing_signature)).send();

                    if (txRcp != null) {
                        listener.onChannelClosed(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onChannelCloseError("Unable to get channel close status!");
                    }
                } catch (Exception e) {
                    listener.onChannelCloseError(e.getMessage());
                }
            }
        });
    }

    //Owner only
    public void addTrustedContracts(List<String> trustedContracts, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.addTrustedContracts(trustedContracts).send();

                    if (txRcp != null) {
                        listener.onTrustedContractsAdded(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onTrustedContractAddError("Unable to add trusted contracts");
                    }
                } catch (Exception e) {
                    listener.onTrustedContractAddError(e.getMessage());
                }
            }
        });
    }

    //Owner only
    public void removeTrustedContracts(List<String> trustedContracts, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.removeTrustedContracts(trustedContracts).send();

                    if (txRcp != null) {
                        listener.onTrustedContractsRemoved(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onTrustedContractRemoveError("Unable to remove trusted contracts");
                    }
                } catch (Exception e) {
                    listener.onTrustedContractRemoveError(e.getMessage());
                }
            }
        });
    }

    public void closeChannelUncooperative(String receiverAddress, long openBlock, double balance, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.uncooperativeClose(receiverAddress, BigInteger.valueOf(openBlock), getWeiValue(balance)).send();

                    if (txRcp != null) {
                        listener.onChannelClosedUncooperative(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onChannelCloseUncooperativeError("Unable to close channel");
                    }
                } catch (Exception e) {
                    listener.onChannelCloseUncooperativeError(e.getMessage());
                }
            }
        });
    }

    public void settleChannel(String receiverAddress, long openBlock, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.settle(receiverAddress, BigInteger.valueOf(openBlock)).send();

                    if (txRcp != null) {
                        listener.onChannelSettled(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onChannelSettleError("Unable to settle channel");
                    }
                } catch (Exception e) {
                    listener.onChannelSettleError(e.getMessage());
                }
            }
        });
    }

    //Trusted contracts only
    public void topUpDelegate(String senderAddress, String receiverAddress, long openBlock, double addedDeposit, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.topUpDelegate(senderAddress, receiverAddress, BigInteger.valueOf(openBlock), getWeiValue(addedDeposit)).send();

                    if (txRcp != null) {
                        listener.onToppedUpDelegate(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onTopUpDelegateError("Unable to top up channel");
                    }
                } catch (Exception e) {
                    listener.onTopUpDelegateError(e.getMessage());
                }
            }
        });
    }

    //Trusted contracts only
    public void createChannelDelegate(String senderAddress, String receiverAddress, double deposit, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RaidenMicroTransferChannels channelManager = loadChannelManager(credentials);
                try {
                    TransactionReceipt txRcp = channelManager.createChannelDelegate(senderAddress, receiverAddress, getWeiValue(deposit)).send();

                    if (txRcp != null) {
                        listener.onChannelCreatedDelegate(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onChannelCreateDelegateError("Unable to create channel");
                    }
                } catch (Exception e) {
                    listener.onChannelCreateDelegateError(e.getMessage());
                }
            }
        });
    }
}
