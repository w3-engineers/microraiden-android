package com.w3engineers.microraidensample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.w3engineers.microraiden.Microraiden;
import com.w3engineers.microraiden.Token;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity implements Microraiden.MicroraidenListener, Token.TokenListener {
    private String TAG = "INFO";
    public static final String tokenAddress = "<DEPLOYED_TOKEN_ADDRESS>";
    public static final String channelAddress = "<DEPLOYED_MICRORAIDEN_CONTRACT_ADDRESS>";
    public static final String web3jRpcURL = "<RPC_ENDPOINT_URL>";

    private long mGasPrice = 60000000000l;//SET GAS PRICE AS REQUIRED
    private long mGasLimit = 8000000; //SET GAS LIMIT AS REQUIRED
    private Microraiden microraiden;
    private Token token;
    private Credentials credentials;

    private String senderAddress = "<SENDER_ETHEREUM_ADDRESS, USER CAN INPUT THIS>";
    private String senderPrivateKey = "<SENDER_PRIVATE_KEY, THIS VALUE WON'T BE STORED LOCALLY FOR SECURITY REASON>";

    private String receiverAddress = "<RECEIVER_ETHEREUM_ADDRESS, USER CAN INPUT THIS>";
    private String receiverPrivateKey = "<RECEIVER_PRIVATE_KEY, THIS VALUE WON'T BE STORED LOCALLY FOR SECURITY REASON>";

    //FOLLOWING VALUES WILL BE INPUT BY USER
    private long channelBlockNumber = 0l;
    private double transferBalance = 3;
    private double approveBalance = 5;
    private double createChannelBalance = 5;

    private String balanceProof = "";
    private String closingHash = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        microraiden = new Microraiden(channelAddress, web3jRpcURL, mGasPrice, mGasLimit, this);
        token = new Token(tokenAddress, web3jRpcURL, mGasPrice, mGasLimit, this);

        initUI();
    }

    @Override
    public void onBalanceWithDrawn(String txStatus, String txHash) {
        if (txStatus.equals("0x1")) {
            Log.i(TAG, "onBalanceWithDrawn  " + txHash);

        } else {
            Log.i(TAG, "onBalanceWithDrawn  failed");
        }
    }

    @Override
    public void onWithDrawnError(String msg) {
        Log.i(TAG, "onWithDrawnError " + msg);
    }

    @Override
    public void onChannelCreated(String txStatus, String txHash, long openBlock) {
        if (txStatus.equals("0x1")) {
            Log.i(TAG, "onChannelCreated  " + txHash + "  BLOCK  " + openBlock);
            channelBlockNumber = openBlock;
        } else {
            Log.i(TAG, "onChannelCreated  failed");
        }
    }

    @Override
    public void onChannelCreateError(String msg) {
        Log.i(TAG, "onChannelCreateError " + msg);
    }

    @Override
    public void onChannelToppedUp(String txStatus, String txHash) {

    }

    @Override
    public void onChannelTopupError(String msg) {

    }

    @Override
    public void onChannelClosed(String txStatus, String txHash) {
        if (txStatus.equals("0x1")) {
            Log.i(TAG, "onChannelClosed  " + txHash);
        } else {
            Log.i(TAG, "onChannelClosed  failed");
        }
    }

    @Override
    public void onChannelCloseError(String msg) {
        Log.i(TAG, "onChannelCloseError  " + msg);
    }

    @Override
    public void onTrustedContractsAdded(String txStatus, String txHash) {

    }

    @Override
    public void onTrustedContractAddError(String msg) {

    }

    @Override
    public void onTrustedContractsRemoved(String txStatus, String txHash) {

    }

    @Override
    public void onTrustedContractRemoveError(String msg) {

    }

    @Override
    public void onChannelClosedUncooperative(String txStatus, String txHash) {

    }

    @Override
    public void onChannelCloseUncooperativeError(String msg) {

    }

    @Override
    public void onChannelSettled(String txStatus, String txHash) {

    }

    @Override
    public void onChannelSettleError(String msg) {

    }

    @Override
    public void onToppedUpDelegate(String txStatus, String txHash) {

    }

    @Override
    public void onTopUpDelegateError(String msg) {

    }

    @Override
    public void onChannelCreatedDelegate(String txStatus, String txHash) {

    }

    @Override
    public void onChannelCreateDelegateError(String msg) {

    }

    @Override
    public void onBalanceApproved(String txStatus, String txHash) {
        if (txStatus.equals("0x1")) {
            Log.i(TAG, "onBalanceApproved  " + txHash);
        } else {
            Log.i(TAG, "onBalanceApproved  failed");
        }
    }

    @Override
    public void onBalanceApproveError(String msg) {
        Log.i(TAG, "onBalanceApproveError  " + msg);
    }

    @Override
    public void onTokenMinted(String txStatus, String txHash) {
        if (txStatus.equals("0x1")) {
            Log.i(TAG, "onTokenMinted  " + txHash);
        } else {
            Log.i(TAG, "onTokenMinted  failed");
        }
    }

    @Override
    public void onTokenMintError(String msg) {
        Log.i(TAG, "onTokenMintError  " + msg);
    }

    @Override
    public void onTokenTransferred(String txStatus, String txHash) {

    }

    @Override
    public void onTokenTransferError(String msg) {

    }

    @Override
    public void onTokenTransferredFrom(String txStatus, String txHash) {

    }

    @Override
    public void onTokenTransferFromError(String msg) {

    }

    @Override
    public void onFundTransferred(String txStatus, String txHash) {

    }

    @Override
    public void onFundTransferError(String msg) {

    }

    private void initUI() {

        Button initSenderBtn = findViewById(R.id.initSenderBtn);

        initSenderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: init sender");
                loadUser(senderPrivateKey);
            }
        });


        Button initReceiverBtn = findViewById(R.id.initReceiverBtn);
        initReceiverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: init Receiver");
                loadUser(receiverPrivateKey);
            }
        });


        Button approveBtn = findViewById(R.id.approveBtn);
        approveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: approve");
                approveBalance(approveBalance, channelAddress);
            }
        });


        Button allowanceBtn = findViewById(R.id.allowanceBtn);
        allowanceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: allowanceBtn");
                getAllowance(senderAddress, channelAddress);
            }
        });


        Button createChannelBtn = findViewById(R.id.createChannelBtn);
        createChannelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: createChannelBtn");
                createChannel(receiverAddress, createChannelBalance);
            }
        });


        Button bpsBtn = findViewById(R.id.bpsBtn);
        bpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: bpsBtn");
                getBalanceProof(receiverAddress, channelBlockNumber, transferBalance);
            }
        });


        Button verifyBalanceProof = findViewById(R.id.verifyBalanceProof);
        verifyBalanceProof.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: verifyBalanceProof");
                verifyBalanceProofSignature(receiverAddress, channelBlockNumber, transferBalance, balanceProof);
            }
        });


        Button chsBtn = findViewById(R.id.chsBtn);
        chsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: chsBtn");
                getClosingSignature(senderAddress, channelBlockNumber, transferBalance);

            }
        });

        Button verifyClosing = findViewById(R.id.verifyClosing);
        verifyClosing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: verifyBalanceProof");
                verifyClosingSignature(senderAddress, channelBlockNumber, transferBalance, closingHash);
            }
        });

        Button channelInforBtn = findViewById(R.id.channelInforBtn);
        channelInforBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: channelInforBtn" + channelBlockNumber);
                getchannelInfo(senderAddress, receiverAddress, channelBlockNumber);
            }
        });

        Button withdrawBtn = findViewById(R.id.withdrawBtn);
        withdrawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: withdrawBtn");
                withdrawBalance(channelBlockNumber, transferBalance, balanceProof);
            }
        });

        Button closeBtn = findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: closeBtn");
                closeChannelCooperative(receiverAddress,  channelBlockNumber, transferBalance, balanceProof, closingHash);
            }
        });

        Button getBalanceBtn = findViewById(R.id.getBalanceBtn);
        getBalanceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: getBalanceBtn");
                getBalanceOf(credentials.getAddress());
            }
        });

        Button mintTokenBtn = findViewById(R.id.mintTokenBtn);
        mintTokenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: mintTokenBtn");
                mintToken();
            }
        });

    }

    private Credentials getCredentialsFromPrivateKey(String key) {
        return Credentials.create(key);
    }

    private void loadUser(String privateKey) {
        credentials = getCredentialsFromPrivateKey(privateKey);
        Log.i(TAG, "Credentials loaded " + credentials.getAddress());
    }

    private void approveBalance(double amount, String spender) {
        token.approveBalance(amount, spender, credentials);
    }

    private void getAllowance(String owner, String spender) {
        try {
            Double allowance = token.getTokenAllowance(owner, spender, credentials);
            if (allowance == null){
                Log.i(TAG, "getAllowance: got null value");
            } else {
                Log.i(TAG, "ALLOWANCE  " + allowance.doubleValue());
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void createChannel(String _receiver_address, double _deposit) {
        microraiden.createChannel(_receiver_address, _deposit, credentials);
    }

    private void getBalanceProof(String receiver_address, long open_block, double balance) {
        balanceProof = microraiden.getBalanceProof(receiver_address, open_block, balance, credentials);
        Log.i(TAG, "BalanceProof : " + balanceProof);
    }

    private void getClosingSignature(String sender_address, long openBlock, double balance) {
        closingHash = microraiden.getClosingHash(sender_address, openBlock, balance, credentials);
        Log.i(TAG, "ClosingHash: " + closingHash);
    }

    private void verifyBalanceProofSignature(String receiver, long openBlock, double balance, String _balanceProof) {
        try {
            String ownerAddress = microraiden.verifyBalanceProofSignature(receiver, openBlock, balance, _balanceProof, credentials);
            Log.i(TAG, "balanceproof verified " + ownerAddress);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void verifyClosingSignature(String sender, long openBlock, double balance, String _closingHash)  {
        try {
            String receiver = receiver = microraiden.verifyClosingHashSignature(sender, openBlock, balance, _closingHash, credentials);
            Log.i(TAG, "closingHash verifyied " + receiver);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getchannelInfo(String sender, String receiver, long openBlock){
        try {
            Tuple5<byte[], Double, Long, Double, Double> chInfo = microraiden.getChannelInfo(sender, receiver, openBlock, credentials);
            Log.i(TAG, "channel Info:\nkey: "+ chInfo.getValue1()+"\ndeposit: " + chInfo.getValue2() +
                    "\nsettle_block_number: "+ chInfo.getValue3() + "\nclosing_balance: " + chInfo.getValue4() + "\nwithdrawn_balance " + chInfo.getValue5());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void withdrawBalance(long openBlock, double balance, String _balanceProof) {
        microraiden.withdrawBalance(openBlock, balance, _balanceProof, credentials);
    }

    public void closeChannelCooperative(String receiver, long openBlock, double balance,
                                          String _balanceProof, String _closingHash) {
        microraiden.closeChannelCooperative(receiver, openBlock, balance, _balanceProof, _closingHash, credentials);
    }

    private void getBalanceOf(String address){

        try {
            Double tknBalance  = token.getTokenBalance(address, credentials);
            if (tknBalance != null){
                Log.i(TAG, "TokenBalance " + tknBalance);
            } else {
                Log.i(TAG, "TokenBalance null");
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void mintToken() {
        token.mintToken(0.1, credentials);
    }



}
