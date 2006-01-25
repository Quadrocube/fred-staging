package freenet.client.async;

import freenet.client.ClientMetadata;
import freenet.client.InsertBlock;
import freenet.client.InserterContext;
import freenet.client.InserterException;
import freenet.client.Metadata;
import freenet.keys.ClientKey;
import freenet.keys.FreenetURI;
import freenet.support.Bucket;
import freenet.support.Logger;

public class ClientPutter extends BaseClientPutter implements PutCompletionCallback {

	final ClientCallback client;
	final Bucket data;
	final FreenetURI targetURI;
	final ClientMetadata cm;
	final InserterContext ctx;
	private ClientPutState currentState;
	private boolean finished;
	private final boolean getCHKOnly;
	private final boolean isMetadata;
	private FreenetURI uri;

	public ClientPutter(ClientCallback client, Bucket data, FreenetURI targetURI, ClientMetadata cm, InserterContext ctx,
			ClientRequestScheduler scheduler, short priorityClass, boolean getCHKOnly, boolean isMetadata) {
		super(priorityClass, scheduler);
		this.cm = cm;
		this.isMetadata = isMetadata;
		this.getCHKOnly = getCHKOnly;
		this.client = client;
		this.data = data;
		this.targetURI = targetURI;
		this.ctx = ctx;
		this.finished = false;
		this.cancelled = false;
	}

	public void start() throws InserterException {
		try {
			currentState =
				new SingleFileInserter(this, this, new InsertBlock(data, cm, targetURI), isMetadata, ctx, false, false, getCHKOnly, false);
			((SingleFileInserter)currentState).start();
		} catch (InserterException e) {
			finished = true;
			currentState = null;
		}
	}

	public void setCurrentState(ClientPutState s) {
		currentState = s;
	}

	public void onSuccess(ClientPutState state) {
		finished = true;
		currentState = null;
		client.onSuccess(this);
	}

	public void onFailure(InserterException e, ClientPutState state) {
		finished = true;
		currentState = null;
		client.onFailure(e, this);
	}

	public void onEncode(ClientKey key, ClientPutState state) {
		this.uri = key.getURI();
		client.onGeneratedURI(uri, this);
	}
	
	public void cancel() {
		synchronized(this) {
			super.cancel();
			if(currentState != null)
				currentState.cancel();
		}
	}
	
	public boolean isFinished() {
		return finished || cancelled;
	}

	public FreenetURI getURI() {
		return uri;
	}

	public void onTransition(ClientPutState oldState, ClientPutState newState) {
		// Ignore
	}

	public void onMetadata(Metadata m, ClientPutState state) {
		Logger.error(this, "Got metadata on "+this+" from "+state+" (this means the metadata won't be inserted)");
	}
	
}
