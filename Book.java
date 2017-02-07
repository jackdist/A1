// specify the package
package model;

// system imports
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JFrame;

// project imports
import exception.InvalidPrimaryKeyException;
import database.*;

import impresario.IView;

import userinterface.View;
import userinterface.ViewFactory;

/** The class containing the Account for the ATM application */
//==============================================================
public class Book extends EntityBase implements IView
{
	private static final String myTableName = "book";

	protected Properties dependencies;

	// GUI Components

	private String updateStatusMessage = "";

	// constructor for this class
	//----------------------------------------------------------
	public Book(String bookId)
		throws InvalidPrimaryKeyException
	{
		super(myTableName);

		setDependencies();
		String query = "SELECT * FROM " + myTableName + " WHERE (bookId = " + bookId + ")";

		Vector<Properties> allDataRetrieved = getSelectQueryResult(query);

		// You must get one account at least
		if (allDataRetrieved != null)
		{
			int size = allDataRetrieved.size();

			// There should be EXACTLY one account. More than that is an error
			if (size != 1)
			{
				throw new InvalidPrimaryKeyException("Multiple book matching id : "
					+ bookId + " found.");
			}
			else
			{
				// copy all the retrieved data into persistent state
				Properties retrievedBookData = allDataRetrieved.elementAt(0);
				persistentState = new Properties();

				Enumeration allKeys = retrievedBookData.propertyNames();
				while (allKeys.hasMoreElements() == true)
				{
					String nextKey = (String)allKeys.nextElement();
					String nextValue = retrievedBookData.getProperty(nextKey);

					if (nextValue != null)
					{
						persistentState.setProperty(nextKey, nextValue);
					}
				}

			}
		}
		// If no account found for this user name, throw an exception
		else
		{
			throw new InvalidPrimaryKeyException("No book matching id : "
				+ bookId + " found.");
		}
	}

	// Can also be used to create a NEW Account (if the system it is part of
	// allows for a new account to be set up)
	//----------------------------------------------------------
	public Book(Properties props)
	{
		super(myTableName);

		setDependencies();
		persistentState = new Properties();
		Enumeration allKeys = props.propertyNames();
		while (allKeys.hasMoreElements() == true)
		{
			String nextKey = (String)allKeys.nextElement();
			String nextValue = props.getProperty(nextKey);

			if (nextValue != null)
			{
				persistentState.setProperty(nextKey, nextValue);
			}
		}
	}

	//-----------------------------------------------------------------------------------
	private void setDependencies()
	{
		dependencies = new Properties();
	
		myRegistry.setDependencies(dependencies);
	}

	//----------------------------------------------------------
	public Object getState(String key)
	{
		if (key.equals("UpdateStatusMessage") == true)
			return updateStatusMessage;

		return persistentState.getProperty(key);
	}

	//----------------------------------------------------------------
	public void stateChangeRequest(String key, Object value)
	{

		myRegistry.updateSubscribers(key, this);
	}

	/** Called via the IView relationship */
	//----------------------------------------------------------
	public void updateState(String key, Object value)
	{
		stateChangeRequest(key, value);
	}

	/**
	 * Verify ownership
	 */
	//----------------------------------------------------------
	public boolean verifyOwnership(AccountHolder cust)
	{
		if (cust == null)
		{
			return false;
		}
		else
		{
			String custid = (String)cust.getState("ID");
			String myOwnerid = (String)getState("OwnerID");
			// DEBUG System.out.println("Account: custid: " + custid + "; ownerid: " + myOwnerid);

			return (custid.equals(myOwnerid));
		}
	}

	/**
	 * Credit balance (Method is public because it may be invoked directly as it has no possibility of callback associated with it)
	 */
	//----------------------------------------------------------
	public void credit(String amount)
	{
		String myBalance = (String)getState("Balance");
		double myBal = Double.parseDouble(myBalance);

		double incrementAmount = Double.parseDouble(amount);
		myBal += incrementAmount;

		persistentState.setProperty("Balance", ""+myBal);
	}

	/**
	 * Debit balance (Method is public because it may be invoked directly as it has no possibility of callback associated with it)
	 */
	//----------------------------------------------------------
	public void debit(String amount)
	{
		String myBalance = (String)getState("Balance");
		double myBal = Double.parseDouble(myBalance);

		double incrementAmount = Double.parseDouble(amount);
		myBal -= incrementAmount;

		persistentState.setProperty("Balance", ""+myBal);
	}

	/**
	 * Check balance -- returns true/false depending on whether
	 * there is enough balance to cover withdrawalAmount or not
	 * (Method is public because it may be invoked directly as it has no possibility of callback associated with it)
	 *
	 */
	//----------------------------------------------------------
	public boolean checkBalance(String withdrawalAmount)
	{
		String myBalance = (String)getState("Balance");
		double myBal = Double.parseDouble(myBalance);

		double checkAmount = Double.parseDouble(withdrawalAmount);

		if (myBal >= checkAmount)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	//----------------------------------------------------------
	public void setServiceCharge(String value)
	{
		persistentState.setProperty("ServiceCharge", value);
		updateStateInDatabase();
	}
	
	//-----------------------------------------------------------------------------------
	public static int compare(Account a, Account b)
	{
		String aNum = (String)a.getState("AccountNumber");
		String bNum = (String)b.getState("AccountNumber");

		return aNum.compareTo(bNum);
	}

	//-----------------------------------------------------------------------------------
	public void update()
	{
		updateStateInDatabase();
	}
	
	//-----------------------------------------------------------------------------------
	private void updateStateInDatabase() 
	{
		try
		{
			if (persistentState.getProperty("bookId") != null)
			{
				Properties whereClause = new Properties();
				whereClause.setProperty("bookId",
				persistentState.getProperty("bookId"));
				updatePersistentState(mySchema, persistentState, whereClause);
				updateStatusMessage = "Book data for bookId : " + persistentState.getProperty("bookId") + " updated successfully in database!";
			}
			else
			{
				Integer accountNumber =
					insertAutoIncrementalPersistentState(mySchema, persistentState);
				persistentState.setProperty("bookId", "" + accountNumber.intValue());
				updateStatusMessage = "Book data for new book : " +  persistentState.getProperty("bookId")
					+ "installed successfully in database!";
			}
		}
		catch (SQLException ex)
		{
			updateStatusMessage = "Error in installing account data in database!";
		}
		//DEBUG System.out.println("updateStateInDatabase " + updateStatusMessage);
	}


	/**
	 * This method is needed solely to enable the Account information to be displayable in a table
	 *
	 */
	//--------------------------------------------------------------------------
	public Vector<String> getEntryListView()
	{
		Vector<String> v = new Vector<String>();

		v.addElement(persistentState.getProperty("AccountNumber"));
		v.addElement(persistentState.getProperty("Type"));
		v.addElement(persistentState.getProperty("Balance"));
		v.addElement(persistentState.getProperty("ServiceCharge"));

		return v;
	}

	//-----------------------------------------------------------------------------------
	protected void initializeSchema(String tableName)
	{
		if (mySchema == null)
		{
			mySchema = getSchemaInfo(tableName);
		}
	}
}

