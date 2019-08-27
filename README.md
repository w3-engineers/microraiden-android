Microraiden-Android
===============

[![](https://jitpack.io/v/w3-engineers/microraiden-android.svg)](https://jitpack.io/#w3-engineers/microraiden-android)

Microraiden-Android is a lightweight, cheap, low-latency modular Java and Android library, 
which works with Smart contract and Ethereum network. This library reduces additional
overhead to write smart contract and own integration code for this platform.

Feature
-------

 - Balance proof signature
 - Closing hash generate
 - Smart contract has been deployed on the main net
 
It has a runtime dependency: 
 - [Web3j](https://github.com/web3j/web3j) for connecting and communicating with blockchain

Microraiden overview
---------------------

Microraiden is many to one unidirectional state channel protocol system and its off-chain transaction do not cost anything, 
exchange confined between sender and receiver.

### Token
Own token uses in Microraiden for payment process. Which is [ERC20](https://github.com/ethereum/EIPs/issues/20) and [ERC223](https://github.com/ethereum/EIPs/issues/223) consequent.

In a nutshell, a client(Sender) wants to access the payable shared resource, and a provider(Receiver) is open to share his resource
and wants to get paid with number of tokens. And a third party (Microraiden) takes the authority to manage every single transaction between them.

### Off-chain transaction
The system lies sender to receiver off-chain transaction. They keep track of the last transaction information 
and channel balance in a secure way. The channel balance is calculated each time on the sender used resource amount. 
Sender prepares a sign(Balance Proof) message that confirms the total amount of token. 
Then the balance proof sends to receiver server. Balance proof comparing the last received balance with sender signature. 
If everything is okay then receiver updates old balance to the new balance.

[µRaiden documentation](https://microraiden.readthedocs.io/) for getting more information about Microraiden

Getting started
---------------

Add it in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

If you are building with Gradle, simply add the following line to the `dependencies` section of your `build.gradle` file:

```
    implementation 'com.github.w3-engineers:microraiden-android:alpha-0.0.1'
```

For using microraiden library your project need to **Java 8 Compatibility** support

- For initializing `Microraiden`, you have to need a channel address, rpc url and set a gas price with a gas limit. 
Provide a `MicroraidenListener` listener during initialization.
```
new Microraiden(channelAddress, web3jRpcURL, mGasPrice, mGasLimit, this)
```
Acquainted with following variables for accessing microraiden API’s

| Variable name | Data type   | Purpose |
| ------------- | -------     | --------- |
| credentials   | Credentials | For block chain transaction, you should use credentials that contains secure information and keys. |
| sender        | String      | Sender can send the payment and consume/ take the service from receiver, sender is an ethereum address. |
| receiver      | String      | Receiver is a service provider and it is also an ethereum address. |
| blockNumber   | Long        | Block chain transaction block number |
| deposit       | Double      | The amount of payment |
| balance       | Double      | Balance is current transfer, is increases in cumulative way adding all previous balances |
| balanceProof  | String      | A list of data composition that contains receiver,  balance, contract, block number and transaction id |
| closingHash   | String      | Compilation of set of information, that contains transaction id, sender, contract and balance |
| addedDeposit  | Double      | The newly added the amount of payment which is used during the top-up functionalities |

For preparing a credentials you have to use private key

```
    Credentials.create(key)
```

For retrieving token address you should use `getTokenAddress` API with credentials

```
    microraiden.getTokenAddress(credentials); 
```

Create a new channel between sender and receiver for transferring the deposit  then you will use `createChannel` API 

```
    microraiden.createChannel(receiver, deposit, credentials); 
```

`getChannelInfo` retrieve the info about channel

```
    microraiden.getChannelInfo(sender, receiver, blockNumber, credentials);
```

`topUpChannel` using for increasing the channel deposit with newly added deposit 

```
    microraiden.topUpChannel(receiver, blockNumber, addedDeposit, credentials); 
```

Receiver will create the balance proof signature  from `getBalanceProof` API with receiver, blockNumber, balance and credentials

```
    microraiden.getBalanceProof(receiver, blockNumber, balance, credentials);
```

`verifyBalanceProofSignature` return the sender address after verifying the balance proof 

```
    microraiden.verifyBalanceProofSignature(receiver, blockNumber, balance, balanceProof, credentials);
```

For preparing closing hash signature send can use `getClosingSignature` with sender, block number, balance and credentials parameters

```
    microraiden.getClosingHash(sender, blockNumber, balance, credentials);
```

`verifyClosingHashSignature` API is helpful for extracting/verifying closing hash signature and provide the receiver address. 
Following property required for extract closing hash, sender address, block number, balance, closing signature and credentials. 

```
    microraiden.verifyClosingHashSignature(sender, blockNumber, balance, closingHash, credentials); 
```

`withdrawBalance` allow for receiver for with the token. For withdrawing balance you should use it 
and set the following parameters blockNumber, balance, balanceProof and credentials.

```
    microraiden.withdrawBalance(blockNumber, balance, balanceProof, credentials);
```

`getChannelDepositLimit` will share the limit of total token deposit in a channel 

```
    microraiden.getChannelDepositLimit(credentials);
```

For getting channel key you should use `getChannelKey` with the set of following properties sender, receiver and blockNumber

```
    microraiden.getChannelKey(sender, receiver, blockNumber, credentials);
```

If you want the owner address then call the `getOwnerAddress`. Then you will get the owner ethereum address.

```
    microraiden.getOwnerAddress(credentials);
```

`settleChannel` will call the sender or receiver when close to the channel and immediate to the settle

```
    microraiden.settleChannel(receiver, blockNumber, credentials);
```

`getVersion` will provide the semantic versioning number to smart contract 

```
    microraiden.getVersion(credentials);
```

`getChallengePeriod` that initiated by sender for waiting an uncooperativeClose 

```
    microraiden.getChallengePeriod(credentials); 
```

`addTrustedContracts` API called only by owner for adding a set of contract in trusted contract list

```
    microraiden.addTrustedContracts(trustedContracts, credentials);
```

`createChannelDelegate` only allow for delegate contract who can create a channel using sender, receiver, deposit amount and credentials

```
    microraiden.createChannelDelegate(sender, receiver, deposit, credentials);
```

`topUpDelegate` only allow for delegate contract who can increase the channel deposit

```
    microraiden.topUpDelegate(sender, receiver, blockNumber, addedDeposit, credentials);
```

`removeTrustedContracts` only called by owner to remove a contract from trusted contract list.

```
    microraiden.removeTrustedContracts(trustedContracts, credentials);
```

`closeChannelCooperative` called by sender, receiver and delegate contracts, when the need to close the channel and settle immediately. 

```
    microraiden.closeChannelCooperative(receiver, blockNumber, balance, balanceProof, closingHash, credentials); 
```

`closeChannelUncooperative` can called by sender with set a challenge period of time

```
    microraiden.closeChannelUncooperative(receiver, blockNumber, balance, credentials);
```