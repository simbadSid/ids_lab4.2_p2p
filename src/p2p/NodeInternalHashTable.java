package p2p;

import java.lang.reflect.Method;
import java.util.LinkedList;





public class NodeInternalHashTable
{
// ---------------------------------
// Attributes
// ---------------------------------
	private int			previousNodeHash	= -1;
	private int			currentNodeHash		= -1;
	private Method		getKeyIndex;
	private String[]	table;
// TODO to remove
	private LinkedList<String> keySet;

// ---------------------------------
// Builder
// ---------------------------------
	public NodeInternalHashTable(int previousNodeHash, int currentNodeHash)
	{
		if (previousNodeHash	< 0)				throw new RuntimeException();
		if (currentNodeHash		< 0)				throw new RuntimeException();
		if (previousNodeHash	== currentNodeHash)	throw new RuntimeException();

		this.previousNodeHash	= previousNodeHash;
		this.currentNodeHash	= currentNodeHash;

		String caseName = null;
		int nbrKey = -1;
		if (currentNodeHash > previousNodeHash)
		{
			caseName	= "case1";
			nbrKey		= this.currentNodeHash - this.previousNodeHash + 1;
		}
		else
		{
			caseName	= "case2";
			nbrKey		= (Node.maxNbrNodes - this.previousNodeHash) + this.currentNodeHash + 1;
		}
		try
		{
			this.getKeyIndex= NodeInternalHashTable.class.getDeclaredMethod("getKeyIndex_"+caseName, int.class);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		this.table	= new String[nbrKey];
		this.keySet	= new LinkedList<String>();
	}

// ---------------------------------
// Local methods
// ---------------------------------
	/**
	 * Implements the SHA-1 algorithm
	 */
//TODO
	public static int hash(String key)
	{
		int res = 0;
		for (int i=0; i<key.length(); i++)
		{
			res += key.charAt(i);
		}
		return (res % 15);
	}

	/**
	 * Insert the given value in the hash table
	 * @return false if the key does not belong to the set of key managed by the current 
	 * hash table (do not insert the value)
	 */
	public boolean insert(String key, String value)
	{
		int hashedKey	= hash(key);
		int tabIndex	= -1;

		try
		{
			tabIndex = (int) this.getKeyIndex.invoke(this, hashedKey);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		if (tabIndex == -1)
			return false;

		this.table[tabIndex] = value;
		this.keySet.addLast(key);
		return true;
	}

	/**
	 * Return the value in the hash table corresponding ...
	 * @return null if the key does not belong to the set of key managed by the current 
	 * hash table
	 */
	public String getValue(String key)
	{
		int hashedKey	= hash(key);
		int tabIndex	= -1;

		try
		{
			tabIndex = (int) this.getKeyIndex.invoke(this, hashedKey);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		if (tabIndex == -1)
			return null;
	
		return this.table[tabIndex];
	}

	/**
	 * @return true if the given key belongs to the key set of the current hashTable
	 */
	public boolean isResponsibleForKey(String key)
	{
		int hashedKey	= hash(key);
		int tabIndex	= -1;

		try
		{
			tabIndex = (int) this.getKeyIndex.invoke(this, hashedKey);
			return (tabIndex != -1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
			return false;
		}
	}

	public LinkedList<String> getKeySet()
	{
		return new LinkedList<String>(this.keySet);
	}

// ---------------------------------
// Private methods
// ---------------------------------
	/**
	 * Applied in the case where the current Node is bigger than the previous</nl>
	 * @return -1 if the key does not belong to the set of key managed by the current 
	 * hash table.   Return the index in the current table otherwise.
	 */
	@SuppressWarnings("unused")
	private int getKeyIndex_case1(int key)
	{
		int res		= key - this.previousNodeHash;
		int nbrKey	= this.currentNodeHash - this.previousNodeHash + 1;

		if (res < 0)		return -1;
		if (res >= nbrKey)	return -1;

		return res;
	}

	/**
	 * Applied in the case where the previousNode is bigger than the current</nl>
	 * @return -1 if the key does not belong to the set of key managed by the current 
	 * hash table.   Return the index in the current table otherwise.
	 */
	@SuppressWarnings("unused")
	private int getKeyIndex_case2(int key)
	{
		if (key >= this.previousNodeHash)
			return key - previousNodeHash;
		else
		{
			if (key > this.currentNodeHash)
				return -1;
			return key;
		}
	}
}