package test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Scanner;

import p2p.EntryThread;
import p2p.Node;
import communication.CommunicationChanel;






public class TestClient
{
// -------------------------------------
// Attributes
// -------------------------------------
	public static String	neighborMatrixFile		= "resource/input/neighborhoodMatrix.txt";
	public static String	communicationChanelType	= CommunicationChanel.COMMUNICATION_CHANEL_RABBITMQ;
	public static Topology	topology;

// -------------------------------------
// Main method
// -------------------------------------
	public static void main(String[] args)
	{
		Scanner sc = new Scanner(System.in);
		TestClient test = new TestClient();
		String nodeIP = null;
		int nodeId = -1;
		topology = new Topology(neighborMatrixFile, communicationChanelType);

/*
		System.out.println("Please enter:");
		System.out.println("\t- \"localhost\" if all the nodes are on the local machine");
		System.out.println("\t- any thing else otherwise:");
		try
		{
			nodeIP = sc.next();
		}
		catch(Exception e)
		{
			sc.close();
			return;
		}
		if (!nodeIP.equals("localhost"))
			nodeIP = null;
*/
nodeIP = "localhost";

		boolean parsed = false;
		while(!parsed)
		{
			System.out.println("\n\nPlease enter:");
			System.out.println("\t- The index of the node to call");
			System.out.println("\t- (-1) to set the node index dynamically");
			try
			{
				nodeId = Integer.parseInt(sc.nextLine());
				if ((nodeId < -1) || (nodeId >= topology.nbrNode()))
					throw new Exception();
				parsed = true;
			}
			catch(Exception e)
			{
				System.out.println("\t**** wrong node id ****");
				continue;
			}
		}

		CommunicationChanel chanel = null;
		while(true)
		{
			System.out.println("\n\nPlease enter");
			System.out.println("\t- \"" + Node.MSG_TYPE_ADD_NEXT+ "\"");
			System.out.println("\t- \"" + Node.MSG_TYPE_SIMPLE_MSG + "\"");
/*			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_GET_NEXT + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_GET_PREVIOUS + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_JOIN + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_INSERT + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_IS_RESPONSIBLE_FOR_KEY + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_GET_VALUE + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_HALT + "\"");
*/
			System.out.println("\t- \"printOverlay\"");
			System.out.println("\t- \"setLocalNode\"");

			String method = sc.next();

			if ((chanel == null) || (chanel.isClose()))
				chanel = connectToNode(sc, nodeIP, nodeId);

			if (method.equals("printOverlay"))
			{
				printOverlay(topology);
				continue;
			}
			if (method.equals("setLocalNode"))
			{
				nodeId = parseNodeId(sc, "use");
				continue;
			}
			Method m = null;
			try
			{
				m = TestClient.class.getDeclaredMethod(method, CommunicationChanel.class, Scanner.class, String.class, int.class, String.class);
			}
			catch (Exception e)
			{
				System.out.println("**** Unhandeled choice ****");
				continue;
			}

			try
			{
				Object res = m.invoke(test, chanel, sc, nodeIP, nodeId, null);
				if (res == null)
				{
					System.out.println("\t**** Request refused by the node ****");
				}
				else
				{
					System.out.println("\tNode response = \"" + res + "\"");
				}
				System.out.println("---------------------------------------");

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	private static void printOverlay(Topology topology)
	{
		for (int i=0; i<topology.nbrNode(); i++)
		{
			Node node = topology.getNode(i);
			System.out.println("\t- Node         : " + i);
			System.out.println("\t- Previous node: " + node.getPrevious());
			System.out.println("\t- Next node    : " + node.getNext());
			System.out.println("\t- Data         : ");
			LinkedList<String> keySet = node.getKeySet();
			if (keySet != null)
			{
				for (String key: keySet)
				{
					System.out.println("\t\t " + key + ":\t" + node.getValue(key));
				}
			}
			System.out.println("\t-------------------------------");
		}
	}

// -------------------------------------
// Local methods
// -------------------------------------
	public Object addNext(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		int		nextNodeId = parseNodeId(sc, "reach next");
		String	nextNodeIP = parseNodeIP(sc, "reach next", nodeIP);

		Object[] arguments = new Object[2];
		arguments[0] = nextNodeId;
		arguments[1] = nextNodeIP;
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_ADD_NEXT, nodeId, arguments);
		return res;
	}

	public Object simpleMsg(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String msg = "";
		int	destNodeId = parseNodeId(sc, "reach next");

		System.out.print("\t\tPlease write the msg = ");
		while(msg.length() == 0)
		{
			msg = sc.nextLine();
		}

		Object[] arguments = new Object[1];
		arguments[0] = msg;
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_SIMPLE_MSG, destNodeId, arguments);
		return res;
	}
/*
	public String getPrevious(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		boolean res = chanel.writeLine(Node.THREAD_EXTERNAL_GET_PREVIOUS);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String getNext(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		boolean res = chanel.writeLine(Node.THREAD_EXTERNAL_GET_NEXT);
		if (!res)
			return null;
		return chanel.readLine();
	}
*/
	public String join(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
// TODO
		return null;
/*
		int		nextNodeId = parseNodeId(sc, "reach next");
		String	nextNodeIP = parseNodeIP(sc, "reach next", null);
		CommunicationChanel newChanel = connectToNode(sc, nextNodeIP, nextNodeId);

		do
		{
			String res = this.isResponsibleForKey(newChanel, sc, nodeIP, nextNodeId, ""+nodeId);
			if ((res != null) && (Boolean.parseBoolean(res)))
			{
				// set previous of .... to -1
				topology.getNode(nextNodeId).setPrevious(-1)
				// get previous previous;
				// Set next of previous to me
				
			}
			String str = this.getNext(newChanel, sc, nodeIP, newNodeId, null);
			if (str == null) break;
			newNodeId = Integer.parseInt(str);
			newChanel = connectToNode(sc, nodeIP, newNodeId);
		}while(newNodeId != nodeId);
		return null;
*/
	}
/*
	public String insert(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key	= "";
		String value= "";

		System.out.print("\t\tPlease write the key: ");
		while(key.length() == 0)
		{
			key = sc.nextLine();
		}
		System.out.print("\t\tPlease write the value: ");
		while(value.length() == 0)
		{
			value = sc.nextLine();
		}

		boolean res = true;
		res &= chanel.writeLine(Node.THREAD_EXTERNAL_INSERT);
		res &= chanel.writeLine(key);
		res &= chanel.writeLine(value);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String isResponsibleForKey(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key;

		if (keyTrash != null)
			key = keyTrash;

		else
		{
			key = "";

			System.out.print("\t\tPlease write the key: ");
			while(key.length() == 0)
			{
				key = sc.nextLine();
			}
		}
		boolean res = true;
		res &= chanel.writeLine(Node.THREAD_EXTERNAL_IS_RESPONSIBLE_FOR_KEY);
		res &= chanel.writeLine(key);
		if (!res)
			return null;
		return chanel.readLine();
	}

	public String getValue(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key;

		if (keyTrash != null)
			key = keyTrash;

		else
		{
			key = "";

			System.out.print("\t\tPlease write the key: ");
			while(key.length() == 0)
			{
				key = sc.nextLine();
			}
		}

		CommunicationChanel newChanel = chanel;
		int newNodeId = nodeId;
		do
		{
			String res = this.isResponsibleForKey(newChanel, sc, nodeIP, newNodeId, key);
			if ((res != null) && (Boolean.parseBoolean(res)))
			{
				boolean test = true;
				test &= newChanel.writeLine(Node.THREAD_EXTERNAL_GET_VALUE);
				test &= newChanel.writeLine(key);
				if (!test)
					break;
				return newChanel.readLine();
			}
			String str = this.getNext(newChanel, sc, nodeIP, newNodeId, null);
			if (str == null) break;
			newNodeId = Integer.parseInt(str);
			if (newNodeId <= 0)
				return "(null)";
			newChanel = connectToNode(sc, nodeIP, newNodeId);
		}while(newNodeId != nodeId);
		return null;
	}

	public boolean halt(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		sc.close();
		System.exit(0);
		return true;
	}
*/

// -------------------------------------
// Private methods
// -------------------------------------
	private static CommunicationChanel connectToNode(Scanner sc, String nodeIP, int nodeId)
	{
		if (nodeIP == null)
		{
			System.out.println("\tPlease enter the IP address of the node to call");
			nodeIP = sc.next();
		}
		if (nodeId < 0)
		{
			nodeId = parseNodeId(sc, "call");
		}

		CommunicationChanel res = EntryThread.connectToNode(false, communicationChanelType, nodeId, nodeId, nodeIP);
		if (res == null)
		{
			System.out.println("\t **** Can't establish connection with node " + nodeId + " at the IP " + nodeIP + "****");
			System.exit(0);
			return null;
		}
		return res;
	}

	private static int parseNodeId (Scanner sc, String nodeType)
	{
		int nodeId;

		while(true)
		{
			System.out.println("\tPlease enter the ID of the node to " + nodeType + " ( >= 0)");
			try
			{
				nodeId = sc.nextInt();
				if (nodeId < 0) throw new Exception();
				return nodeId;
			}
			catch(Exception e)
			{
				System.out.println("\t**** The node ID must be >= 0****");
			}
		}

	}

	private static String parseNodeIP (Scanner sc, String nodeType, String nodeIP)
	{
		String resNodeIP;

		if (nodeIP != null)
			return new String(nodeIP);
		while(true)
		{
			System.out.println("\tPlease enter the IP of the node to " + nodeType);
			try
			{
				resNodeIP = sc.next();
				return resNodeIP;
			}
			catch(Exception e)
			{
				System.exit(0);
			}
		}

	}
}
