package p2p;

import java.io.Serializable;
import java.util.LinkedList;




public class RequestPacket implements Serializable
{
// ----------------------------------
// Attributes
// ----------------------------------
	private static final long serialVersionUID = 1L;

	public String				msgType;
	public int					destNodeId;
	public String				msgId;
	public int					nbrHope;
	public LinkedList<Object>	arguments;

// ----------------------------------
// Builder
// ----------------------------------
	public RequestPacket(String msgType, int destNodeId, String msgId, int nbrHope, LinkedList<Object> arguments)
	{
		this.msgType	= new String(msgType);
		this.destNodeId	= destNodeId;
		this.msgId		= new String(msgId);
		this.nbrHope	= nbrHope;
		this.arguments	= (arguments == null)? null : new LinkedList<Object>(arguments);
	}
}
