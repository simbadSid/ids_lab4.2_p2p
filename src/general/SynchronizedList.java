package general;

import java.util.LinkedList;





public class SynchronizedList<T>
{
// ------------------------------------
// Attributes
// ------------------------------------
	private Object			lock;
	private LinkedList<T>	list;

// ------------------------------------
// Builder
// ------------------------------------
	public SynchronizedList()
	{
		this.lock	= new Object();
		this.list	= new LinkedList<T>();
	}

// ------------------------------------
// Local method
// ------------------------------------
	public void addLast(T elem)
	{
		synchronized (this.lock)
		{
			this.list.addLast(elem);
			this.lock.notifyAll();
		}
	}

	/**
	 * @return the first element in the list and removes it.
	 * If no element is in the list, waits until...
	 */
	public T getAndRemoveFirst()
	{
		T res = null;

		synchronized (this.lock)
		{
			while(this.list.isEmpty())
			{
				try
				{
					this.lock.wait();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
					System.exit(0);
				}
			}
			res = this.list.getFirst();
			this.list.removeFirst();
			this.lock.notifyAll();
		}
		return res;
	}

	public boolean remove(T elem)
	{
		boolean res = false;
		synchronized (this.lock)
		{
			for (int i=0; i<this.list.size(); i++)
			{
				T knownElem = this.list.get(i);
				if (((elem == null) || (knownElem == null))
					||
					(elem.equals(knownElem)))
				{
					this.list.remove(i);
					res =  true;
					break;
				}
			}
			this.lock.notifyAll();
		}
		return res;
	}

	public boolean contains(T elem)
	{
		synchronized (this.lock)
		{
			for (int i=0; i<this.list.size(); i++)
			{
				T knownElem = this.list.get(i);
				if (((elem == null) || (knownElem == null))
					||
					(elem.equals(knownElem)))
				{
					return true;
				}
			}
			this.lock.notifyAll();
		}
		return false;
	}
}