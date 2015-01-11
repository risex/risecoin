package rise.http;

import java.nio.ByteBuffer;

import javax.servlet.http.HttpServletRequest;

import rise.Block;
import rise.Rise;
import rise.util.Convert;
import fr.cryptohash.Shabal256;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetMiningInfo extends APIServlet.APIRequestHandler {
	static final GetMiningInfo instance = new GetMiningInfo();
	
	private GetMiningInfo() {
		super(new APITag[] {APITag.MINING, APITag.INFO});
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
		JSONObject response = new JSONObject();
		
		response.put("height", Long.toString(Rise.getBlockchain().getLastHDDBlock().getHeight() + 1));
		
		Block lastBlock = Rise.getBlockchain().getLastHDDBlock();
		byte[] lastGenSig = lastBlock.getGenerationSignature();
		Long lastGenerator = lastBlock.getGeneratorId();
		
		ByteBuffer buf = ByteBuffer.allocate(32 + 8);
		buf.put(lastGenSig);
		buf.putLong(lastGenerator);
		
		Shabal256 md = new Shabal256();
		md.update(buf.array());
		byte[] newGenSig = md.digest();
		
		response.put("generationSignature", Convert.toHexString(newGenSig));
		response.put("baseTarget", Long.toString(lastBlock.getBaseTarget()));
		
		return response;
	}
}
