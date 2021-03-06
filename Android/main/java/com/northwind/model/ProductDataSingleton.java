package com.northwind.model;

import android.content.Context;
import android.util.Log;

import com.northwind.nwandroid.R;
import com.northwind.services.ODataOnlineManager;
import com.northwind.services.ODataOpenListener;
import com.northwind.services.OnlineODataStoreException;
import com.sap.smp.client.odata.ODataEntity;
import com.sap.smp.client.odata.ODataEntitySet;
import com.sap.smp.client.odata.ODataPropMap;
import com.sap.smp.client.odata.ODataProperty;
import com.sap.smp.client.odata.online.OnlineODataStore;
import com.sap.smp.client.odata.store.ODataResponseSingle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton to hold all product data in the NorthwindApp class. It is kept as a member of
 * the NorthwindApp class as a global variable.
 */
public class ProductDataSingleton
{
    private static final String TAG = "ProductDataSingleton";
    private static ProductDataSingleton instance;
    private static Context appContext;

    /** ODataOnlineManager */
    ODataOnlineManager odataOnlineManager = new ODataOnlineManager();

    /** An array of Product items - used for the master (list) view.
     *  This is the same variable name and type as generated by the Android Studio template.
     */
    public static List<Product> ITEMS;

    /** A map of Product items, the key used is ProductID, but can be changed.
     * This is the same variable name generated by the Android Studio template.
     */
    public static final Map<String, Product> ITEM_MAP = new HashMap<>();

    /** String Array for List (Master) view */
    public static String[] listItems;

    /**
     * Create the Singleton instance
     */
    public static void createInstance()
    {
        if (instance == null) {
            // Create the instance
            instance = new ProductDataSingleton();
        }
    }

    /**
     * Initialize the Singleton with data from the OData service
     * @param context App context
     */
    public static void initialize(Context context)
    {
        appContext = context;
        createOnlineManager(context);
        getTheData();
    }

    /**
     * Method to create the ODataOnlineManager
     * @param context App context
     * @see ODataOnlineManager
     */
    private static void createOnlineManager(Context context)
    {
        try
        {
            ODataOnlineManager.openOnlineStore(context);

        }
        catch (OnlineODataStoreException e)
        {
            e.printStackTrace();
            Log.e(TAG, context.getString(R.string.unable_to_open_store));
        }

    }

    /**
     * Helper method to retrieve the data from the OData Response and
     * store it in the ODataTestActivity productList variable.
     */
    private static void getTheData()
    {
        try
        {
            ITEMS = getProducts();
        }
        catch (OnlineODataStoreException e)
        {
            Log.e(TAG, "caught OnlineODataStoreException");
        }
    }

    /**
     * Create String Array for master (List) view and store the Product objects in a HashMap
     */
    private static void storeData()
    {
        listItems = new String[ITEMS.size()];
        int i = 0;

        for (Product element : ITEMS)
        {
            //Store Product objects in a HashMap
            ITEM_MAP.put(element.getProductID(), element);

            //Store product name in a String array
            listItems[i] = element.getProductName();
            i++;
        }
        Log.i(TAG, String.format("Stored %d items in HashMap", i));

    }


    /**
     * Method to request and extract the data from the OData response and create and Array of Product objects.
     * A do-while loop is used to loop until all entites have been received from the service.
     * In the for loop, the objects are created and the setter methods used to store the info
     * Once the list is complete, we add the Product objects to a Hashmap for rapid retrieval of
     * product details when showing the detail view.
     * The method returns an ArrayList of Product objects.
     * <p></p>
     * See notes in the source code on creating the OData Resource Path, and using the SDK
     * @return ArrayList of Product objects
     * @throws OnlineODataStoreException Thrown for errors in OData communication
     * @see Product
     */
    private static ArrayList<Product> getProducts() throws OnlineODataStoreException
    {
        ArrayList<Product> pList = new ArrayList<>();

        //Get the open online store
        ODataOpenListener openListener = ODataOpenListener.getInstance();
        OnlineODataStore store = openListener.getStore();

        if (store!=null)
        {
            Product product;
            ODataProperty property;
            ODataPropMap properties;
            String resourcePath;

            try
            {
                /**
                 * Build the initial OData resource path and query options string from:
                 *  Collection ID: EntitySet Name
                 *  ?$orderby=   : query option that specifies the order (sorting) of the response
                 *  ProductID    : sort key for the orderby directive
                 *
                 *  Instead of ProductID, you can specify Collections.PRODUCT_NAME to have all products
                 *  returned alphabetically (and by default displayed that way in the list view).
                 *
                 *  The Northwind service enforces server-side paging and will return 20 entities per
                 *  request. To view the paging size ("$skiptoken=") value, paste the first URL below into a
                 *  browser window, and scroll to the bottom of the response. You do not have to
                 *  track the number of entities received in your code. The SDK will create the
                 *  next resource path string for you (as seen in the code below). When the returned
                 *  string is `null' you have received all entities.
                 *
                 *  Since there are 77 entities in the OData service we are calling, the do-while loop
                 *  will end up issuing the four requests below to receive all the data (20 entities at a time).
                 *   - http://services.odata.org/V2/Northwind/Northwind.svc/Products?$orderby=ProductID
                 *   - http://services.odata.org/V2/Northwind/Northwind.svc/Products?$orderby=ProductID&$skiptoken=20,20
                 *   - http://services.odata.org/V2/Northwind/Northwind.svc/Products?$orderby=ProductID&$skiptoken=40,40
                 *   - http://services.odata.org/V2/Northwind/Northwind.svc/Products?$orderby=ProductID&$skiptoken=60,60
                 */

                // Build initial resource path and query options string
                resourcePath = Collections.PRODUCT_COLLECTION + "?$orderby=" + Collections.PRODUCT_ID;

                // Loop until resourcePath is null
                do
                {
                    Log.d(TAG, "Requesting: " + resourcePath);

                    //Executor method for reading an Entity set
                    ODataResponseSingle resp = store.executeReadEntitySet(resourcePath, null);

                    //Get the response payload
                    ODataEntitySet feed = (ODataEntitySet) resp.getPayload();

                    //Get the list of ODataEntity
                    List<ODataEntity> entities = feed.getEntities();

                    //Loop to retrieve the information from the response and store in the Product Object
                    for (ODataEntity entity : entities)
                    {
                        properties = entity.getProperties();
                        property = properties.get(com.northwind.model.Collections.PRODUCT_ID);
                        product = new Product(property.getValue().toString());
                        property = properties.get(Collections.PRODUCT_NAME);
                        product.setProductName(property.getValue().toString());
                        property = properties.get(Collections.SUPPLIER_ID);
                        product.setSupplierID(property.getValue().toString());
                        property = properties.get(Collections.CATEGORY_ID);
                        product.setCategoryID(property.getValue().toString());
                        property = properties.get(Collections.QUANTITY_PER_UNIT);
                        product.setQtyPerUnit(property.getValue().toString());
                        property = properties.get(Collections.UNITS_IN_STOCK);
                        product.setUnitsInStock(property.getValue().toString());
                        property = properties.get(Collections.UNITS_ON_ORDER);
                        product.setUnitsOnOrder(property.getValue().toString());
                        property = properties.get(Collections.REORDER_LEVEL);
                        product.setReorderLevel(property.getValue().toString());
                        property = properties.get(Collections.DISCONTINUED);
                        product.setDiscontinued(property.getValue().toString());

                        /**
                         * The unit price value from the Northwind OData service has
                         * four decimal places, we'll round to two decimal places before storing the data
                         */
                        property = properties.get(Collections.UNIT_PRICE);
                        Float price = Float.valueOf(property.getValue().toString());
                        product.setUnitPrice(String.format("%.2f", price));

                        // Add this entity to the array
                        pList.add(product);
                    }

                    // Get the next resource path from the OData SDK. The call to getNextResourcePath()
                    // will return the appropriate string based on the server's skiptoken value.
                    resourcePath = feed.getNextResourcePath();

                }
                // Short circuit evaluation of the string to see if we should loop again.
                // When all entities have been received, resourcePath will be null
                while (resourcePath != null && !resourcePath.isEmpty());

                // Save a reference to the list in ITEMS.
                ITEMS = pList;

                // Store all items in ITEMS (ArrayList) in ITEM_MAP (HashMap) for rapid retrieval
                storeData();

            }
            catch (Exception e)
            {
                Log.e(TAG, appContext.getString(R.string.online_odata_store_exception));
                throw new OnlineODataStoreException(e);
            }
        }
        else
            Log.e(TAG, "Store not open");

        return pList;
    }

    /**
     * Returns an instance of the ProductDataSingleton
     * @return Instance
     */
    public static ProductDataSingleton getInstance()
    {
        // Return the instance
        return instance;
    }

    /**
     * Private constructor (Singleton)
     */
    private ProductDataSingleton()
    {
        // Constructor hidden because this is a singleton
    }


}
