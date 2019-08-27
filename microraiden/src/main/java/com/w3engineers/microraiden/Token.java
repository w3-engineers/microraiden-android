package com.w3engineers.microraiden;

import android.util.Log;

import com.w3engineers.microraiden.contracts.CustomToken;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Token {
    private static Token instance;

    private Executor executor;
    private ExecutorService callableExecutor;
    private ContractGasProvider contractGasProvider;
    private Web3j web3j;
    private TokenListener listener;

    private static String tokenAddress = null;

    public Token(String contractAddress, String rpcUrl, long gasPrice, long gasLimit, TokenListener tokenListener){
        this.executor = Executors.newSingleThreadExecutor();
        this.callableExecutor = Executors.newFixedThreadPool(1);
        this.listener = tokenListener;

        this.tokenAddress = contractAddress;

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

    public CustomToken loadCustomToken(Credentials credentials) {
        return CustomToken.load(tokenAddress, web3j, credentials, contractGasProvider);
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

    public Double getTokenAllowance(String owner, String spender, Credentials credentials) throws ExecutionException, InterruptedException {

        Future<Double> future = callableExecutor.submit(new Callable() {
            @Override
            public Double call() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    BigInteger allowance = customToken.allowance(owner, spender).send();
                    if (allowance != null) {
                        double tokenValue = getETHorTOKEN(allowance);
                        return tokenValue;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;

            }
        });
        return future.get();
    }

    public Double getTokenBalance(String address, Credentials credentials) throws ExecutionException, InterruptedException {

        Future<Double> future = callableExecutor.submit(new Callable() {
            @Override
            public Double call() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    BigInteger balance = customToken.balanceOf(address).send();
                    if (balance != null) {
                        double tokenValue = getETHorTOKEN(balance);
                        return tokenValue;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;

            }
        });

        return future.get();
    }

    public String getTokenName(Credentials credentials) throws ExecutionException, InterruptedException {

        Future<String> future = callableExecutor.submit(new Callable() {
            @Override
            public String call() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    String name = customToken.name().send();
                    if (name != null) {
                        return name;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }

    public Double getTotalSupply(Credentials credentials) throws ExecutionException, InterruptedException {

        Future<Double> future = callableExecutor.submit(new Callable() {
            @Override
            public Double call() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    BigInteger totalSupply = customToken.totalSupply().send();
                    if (totalSupply != null) {
                        double supply = getETHorTOKEN(totalSupply);
                        return supply;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;

            }
        });

        return future.get();
    }

    public Integer getDecimals(Credentials credentials) throws ExecutionException, InterruptedException {

        Future<Integer> future = callableExecutor.submit(new Callable() {
            @Override
            public Integer call() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    BigInteger decimals = customToken.decimals().send();
                    if (decimals != null) {

                        return decimals.intValue();
                    }
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
                    CustomToken customToken = loadCustomToken(credentials);
                    String address = customToken.owner_address().send();
                    return address;

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
                    CustomToken customToken = loadCustomToken(credentials);
                    String version = customToken.version().send();
                    return version;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }

    public String getTokenSymbol(Credentials credentials) throws ExecutionException, InterruptedException {
        Future<String> future = callableExecutor.submit(new Callable() {
            @Override
            public String call() throws Exception {
                try {
                    CustomToken customToken = loadCustomToken(credentials);
                    String symbol = customToken.symbol().send();
                    return symbol;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        return future.get();
    }

    public interface TokenListener {
        void onBalanceApproved(String txStatus, String txHash);
        void onBalanceApproveError(String msg);

        void onTokenMinted(String txStatus, String txHash);
        void onTokenMintError(String msg);

        void onTokenTransferred(String txStatus, String txHash);
        void onTokenTransferError(String msg);

        void onTokenTransferredFrom(String txStatus, String txHash);
        void onTokenTransferFromError(String msg);

        void onFundTransferred(String txStatus, String txHash);
        void onFundTransferError(String msg);
    }

    public void approveBalance(final double allowance, final String spender, Credentials credentials) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    TransactionReceipt txRcp = customToken.approve(spender, getWeiValue(allowance)).send();

                    if (txRcp != null) {
                        listener.onBalanceApproved(txRcp.getStatus(), txRcp.getTransactionHash());
                    } else {
                        listener.onBalanceApproveError("Unable to get balance approval status!");
                    }
                } catch (Exception e) {
                    listener.onBalanceApproveError(e.getMessage());
                }
            }
        });
    }

    public void mintToken(double amount, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    BigInteger weiValue = getWeiValue(amount);
                    TransactionReceipt txRcp = customToken.mint(weiValue).send();
                    if (txRcp != null) {
                        listener.onTokenMinted(txRcp.getStatus(), txRcp.getTransactionHash());
                    }else {
                        listener.onTokenMintError("Token mint error");
                    }
                } catch (Exception e) {
                    listener.onTokenMintError(e.getMessage());
                }
            }
        });
    }

    public void transfer(String to, double value, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    BigInteger weiValue = getWeiValue(value);
                    TransactionReceipt txRcp = customToken.transfer(to, weiValue).send();
                    if (txRcp != null) {
                        listener.onTokenTransferred(txRcp.getStatus(), txRcp.getTransactionHash());
                    }else {
                        listener.onTokenTransferError("Token mint error");
                    }
                } catch (Exception e) {
                    listener.onTokenTransferError(e.getMessage());
                }
            }
        });
    }

    public void transferFrom(String from, String to, double value, Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                CustomToken customToken = loadCustomToken(credentials);
                try {
                    BigInteger weiValue = getWeiValue(value);
                    TransactionReceipt txRcp = customToken.transferFrom(from, to, weiValue).send();
                    if (txRcp != null) {
                        listener.onTokenTransferredFrom(txRcp.getStatus(), txRcp.getTransactionHash());
                    }else {
                        listener.onTokenTransferFromError("Token mint error");
                    }
                } catch (Exception e) {
                    listener.onTokenTransferFromError(e.getMessage());
                }
            }
        });
    }

    //owner only
    public void transferFunds(Credentials credentials) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                CustomToken customToken = loadCustomToken(credentials);
                try {

                    TransactionReceipt txRcp = customToken.transferFunds().send();
                    if (txRcp != null) {
                        listener.onFundTransferred(txRcp.getStatus(), txRcp.getTransactionHash());
                    }else {
                        listener.onFundTransferError("Token mint error");
                    }
                } catch (Exception e) {
                    listener.onFundTransferError(e.getMessage());
                }
            }
        });
    }
}
