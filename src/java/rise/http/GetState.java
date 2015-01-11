package rise.http;

import rise.Account;
import rise.Alias;
import rise.Asset;
import rise.AssetTransfer;
import rise.Constants;
import rise.db.DbIterator;
import rise.Generator;
import rise.Rise;
import rise.Order;
import rise.Trade;
import rise.peer.Peer;
import rise.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetState extends APIServlet.APIRequestHandler {

    static final GetState instance = new GetState();

    private GetState() {
        super(new APITag[] {APITag.INFO}, "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        response.put("application", Rise.APPLICATION);
        response.put("version", Rise.VERSION);
        response.put("time", Rise.getEpochTime());
        response.put("lastBlock", Rise.getBlockchain().getLastBlock().getStringId());
        response.put("cumulativeDifficulty", Rise.getBlockchain().getLastBlock().getCumulativeDifficulty().toString());

        
        long totalEffectiveBalance = 0;
        try (DbIterator<Account> accounts = Account.getAllAccounts(0, -1)) {
            for (Account account : accounts) {
                long effectiveBalanceNXT = account.getBalanceNQT();
                if (effectiveBalanceNXT > 0) {
                    totalEffectiveBalance += effectiveBalanceNXT;
                }
            }
        }
        response.put("totalEffectiveBalanceNXT", totalEffectiveBalance / Constants.ONE_RISE);
        

        if (!"false".equalsIgnoreCase(req.getParameter("includeCounts"))) {
            response.put("numberOfBlocks", Rise.getBlockchain().getHeight() + 1);
            response.put("numberOfTransactions", Rise.getBlockchain().getTransactionCount());
            response.put("numberOfAccounts", Account.getCount());
            response.put("numberOfAssets", Asset.getCount());
            int askCount = Order.Ask.getCount();
            int bidCount = Order.Bid.getCount();
            response.put("numberOfOrders", askCount + bidCount);
            response.put("numberOfAskOrders", askCount);
            response.put("numberOfBidOrders", bidCount);
            response.put("numberOfTrades", Trade.getCount());
            response.put("numberOfTransfers", AssetTransfer.getCount());
            response.put("numberOfAliases", Alias.getCount());
            //response.put("numberOfPolls", Poll.getCount());
            //response.put("numberOfVotes", Vote.getCount());
        }
        response.put("numberOfPeers", Peers.getAllPeers().size());
        response.put("numberOfUnlockedAccounts", Generator.getAllGenerators().size());
        Peer lastBlockchainFeeder = Rise.getBlockchainProcessor().getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("lastBlockchainFeederHeight", Rise.getBlockchainProcessor().getLastBlockchainFeederHeight());
        response.put("isScanning", Rise.getBlockchainProcessor().isScanning());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());

        return response;
    }

}
