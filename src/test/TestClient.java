package test;

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
//	public static String	neighborMatrixFile		= "resource/input/neighborhoodMatrix_loop.txt";
	public static String	neighborMatrixFile		= "resource/input/neighborhoodMatrix_perso.txt";
//	public static String	neighborMatrixFile		= "resource/input/neighborhoodMatrix_persoForJoin.txt";
//	public static String	neighborMatrixFile		= "resource/input/neighborhoodMatrix_topic.txt";
//	public static String	neighborMatrixFile		= "resource/input/neighborhoodMatrix_ring.txt";
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
				System.out.print("\t choice = ");
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
			System.out.println("\t- \"" + Node.MSG_TYPE_GET_NEXT + "\"");
			System.out.println("\t- \"" + Node.MSG_TYPE_GET_PREVIOUS + "\"");
			System.out.println("\t- \"" + Node.MSG_TYPE_CHORD_SET_NEXT + "\"");
			System.out.println("\t- \"" + Node.MSG_TYPE_CHORD_IS_RESPONSIBLE_FOR_KEY + "\"");
			System.out.println("\t- \"" + Node.MSG_TYPE_CHORD_GET_RESPONSIBLE_FOR_KEY + "\"");
			System.out.println("\t- \"" + Node.MSG_TYPE_CHORD_INSERT + "\"");
			System.out.println("\t- \"" + Node.MSG_TYPE_CHORD_JOIN+ "\"");
/*			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_GET_VALUE + "\"");
			System.out.println("\t- \"" + Node.THREAD_EXTERNAL_HALT + "\"");
*/
			System.out.println("\t- \"printOverlay\"");
			System.out.println("\t- \"setLocalNode\"");

			System.out.print("\t Choice: ");
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
				chanel = null;
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
			System.out.println("\t- Node                  : " + i);
			System.out.println("\t- Previous node         : " + node.getPrevious());
			System.out.println("\t- Previous node (chord) : " + node.getChordPrevious());
			System.out.println("\t- Next node             : " + node.getNext());
			System.out.println("\t- Next node(chord)      : " + node.getChordNext());
			System.out.println("\t- Data                  : ");
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

		LinkedList<Object> arguments = new LinkedList<Object>();
		arguments.add(nextNodeId);
		arguments.add(nextNodeIP);
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_ADD_NEXT, nodeId, arguments);
		return res;
	}

	public Object simpleMsg(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String msg = "";
		int	destNodeId = parseNodeId(sc, "reach next");

		System.out.println("\t Please write the msg");
		System.out.print  ("\t Choice: ");
		while(msg.length() == 0)
		{
			msg = sc.nextLine();
		}

		LinkedList<Object> arguments = new LinkedList<Object>();
		arguments.add(msg);
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_SIMPLE_MSG, destNodeId, arguments);
		return res;
	}

	public Object getPrevious(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_GET_PREVIOUS, nodeId, null);
		return res;
	}

	public Object getNext(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_GET_NEXT, nodeId, null);
		return res;
	}

	public Object chord_setNext(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		int	nextChord = parseNodeId(sc, "reach next (chord)");
		LinkedList<Object> arguments = new LinkedList<Object>();
		arguments.add(nextChord);
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_CHORD_SET_NEXT, nodeId, arguments);
		return res;
	}

	public Object chord_isResponsibleForKey(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key;

		int	nodeToCheck = parseNodeId(sc, "check (chord)");

		if (keyTrash != null)
			key = keyTrash;

		else
		{
			key = "";

			System.out.println("\n\t Please write the key: ");
			System.out.print("\t choice: ");
			while(key.length() == 0)
			{
				key = sc.nextLine();
			}
		}
		LinkedList<Object> arguments = new LinkedList<Object>();
		arguments.add(key);
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_CHORD_IS_RESPONSIBLE_FOR_KEY, nodeToCheck, arguments);
		return res;
	}

	public Object chord_getResponsibleForKey(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key;

		if (keyTrash != null)
			key = keyTrash;

		else
		{
			key = "";

			System.out.println("\n\t Please write the key: ");
			System.out.print("\t choice: ");
			while(key.length() == 0)
			{
				key = sc.nextLine();
			}
		}
		LinkedList<Object> arguments = new LinkedList<Object>();
		arguments.add(key);
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_CHORD_GET_RESPONSIBLE_FOR_KEY, nodeId, arguments);
		return res;
	}

	public Object chord_insert(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		String key	= "";
		String value= "";

		System.out.println("\n\t Please write the key: ");
		System.out.print("\t choice: ");
		while(key.length() == 0)
		{
			key = sc.nextLine();
		}
		System.out.println("\t Please write the value: ");
		System.out.print("\t choice: ");
		while(value.length() == 0)
		{
			value = sc.nextLine();
		}

		LinkedList<Object> arguments = new LinkedList<Object>();
		arguments.add(key);
		arguments.add(value);
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_CHORD_INSERT, nodeId, arguments);
		return res;
	}

	public Object chord_join(CommunicationChanel chanel, Scanner sc, String nodeIP, int nodeId, String keyTrash)
	{
		int	nodeInit = parseNodeId(sc, "init the process (chord)");
		LinkedList<Object> arguments = new LinkedList<Object>();
		arguments.add(nodeInit);
		Object res = EntryThread.sendActionRequestToNode(chanel, nodeId, Node.MSG_TYPE_CHORD_JOIN, nodeId, arguments);
		return res;

	}

// -------------------------------------
// Private methods
// -------------------------------------
	private static CommunicationChanel connectToNode(Scanner sc, String nodeIP, int nodeId)
	{
		if (nodeIP == null)
		{
			System.out.println("\n\t Please enter the IP address of the node to call: ");
			System.out.print("\t Choice: ");
			nodeIP = sc.next();
		}
		if (nodeId < 0)
		{
			nodeId = parseNodeId(sc, "call");
		}
//TODO replace the true by false
		CommunicationChanel res = EntryThread.connectToNode(true, communicationChanelType, nodeId, nodeId, nodeIP);
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
			System.out.println("\n\t Please enter the ID of the node to " + nodeType + " ( >= 0)");
			System.out.print("\t Choice: ");
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
			System.out.println("\n\t Please enter the IP of the node to " + nodeType);
			System.out.print("\t Choice: ");
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
