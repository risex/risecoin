package rise.http;

public enum APITag {

    ACCOUNTS("Accounts"), ALIASES("Aliases"), AE("Asset Exchange"), CREATE_TRANSACTION("Create Transaction"),
    BLOCKS("Blocks"), DGS("Digital Goods Store"), FORGING("Forging"), INFO("Server Info"), MESSAGES("Messages"),
    MINING("Mining"), TRANSACTIONS("Transactions"), TOKENS("Tokens"), VS("Voting System"), SEARCH("Search"), UTILS("Utils"), DEBUG("Debug");

    private final String displayName;

    private APITag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
