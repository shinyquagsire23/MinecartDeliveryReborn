/*
 * MinecartDelivery - Bukkit plugin to allow automated storage minecart deliveries
 * 
 * Copyright 2011 John Paul Alcala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mcd;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DetectorRailBlockListener implements Listener {
    private DropoffPointListener dropoffPointListener;

    public void setDropoffPointListener(DropoffPointListener dropoffPointListener) {
        this.dropoffPointListener = dropoffPointListener;
    }
    @EventHandler
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        Block detectorRail=event.getBlock();

        if(detectorRail==null || Material.DETECTOR_RAIL!=detectorRail.getType()) {
            return;
        }

        Location triggerLocation=detectorRail.getLocation();
        DropoffPoint dropoffPoint=this.dropoffPointListener.getDropoffPoints().get(triggerLocation.toString());


        //System.out.println((dropoffPoint == null) + " " + (event.getNewCurrent()==1));
        //System.out.println(detectorRail.getChunk().getX() + " " + detectorRail.getChunk().getZ());
        if(dropoffPoint!=null && event.getNewCurrent()==1) {
            lookForMinecart(event, dropoffPoint);
        }
    }
    private void lookForMinecart(BlockRedstoneEvent event, DropoffPoint dropoffPoint) {
        Block block=event.getBlock();
        Chunk chunk=block.getChunk();
        Entity entities[]=chunk.getEntities();

        if(entities!=null) 
        {
        	//System.out.println("nullifier");
        	System.out.println(entities.length);
            for(Entity entity:entities) 
            {
            	//System.out.println("entity");
            	//System.out.println(entity.getEntityId());
                if(isValidStorageMinecart(entity, block.getLocation())) 
                {
                	//System.out.println("valid");
                    StorageMinecart storageMinecart=(StorageMinecart) entity;
                    Location torchloc = block.getLocation();
                    Block scanBlock=torchloc.clone().add(0.0, -2.0, 0.0).getBlock();
                    //System.out.println(scanBlock.getX() + " " + scanBlock.getZ() + " " + scanBlock.getY());
                    if(scanBlock.getBlockPower() > 0)
                    	addItemsToDropoff(storageMinecart, dropoffPoint,false);
                    else addItemsToDropoff(storageMinecart,dropoffPoint,true);
                }
            }
        }
        //else
        	//System.out.println("nullifier");
    }
    
    @SuppressWarnings("unused")
	private enum DropOffType {CHEST,FURNACE,DISPENSER};
    private void addItemsToDropoff(StorageMinecart storageMinecart, DropoffPoint dropoffPoint,	boolean from)
    {
    	List<Location> chestLocations=dropoffPoint.getChestLocations();
        List<Chest> dropoffChests=new ArrayList<Chest>();
        List<Furnace> furnaces = new ArrayList<Furnace>();
        List<Dispenser> dispensers = new ArrayList<Dispenser>();

        for(Location location:chestLocations) 
        {
        	if(location.getBlock().getType() == Material.CHEST)
        		dropoffChests.add((Chest) location.getBlock().getState());
        	else if (location.getBlock().getType() == Material.FURNACE)
        		furnaces.add((Furnace) location.getBlock().getState());
        	else if (location.getBlock().getType() == Material.DISPENSER)
        		dispensers.add((Dispenser) location.getBlock().getState());
        }
        if(from)
        {
            if(dropoffChests.size() > 0)
            	transferItemsFromDropoffPoint(storageMinecart, dropoffPoint);
            if(furnaces.size() > 0)
            	transferItemsFromFurnace(storageMinecart, dropoffPoint);
        }
        else
        {
        if(dropoffChests.size() > 0)
        	transferItemsToDropoffPoint(storageMinecart, dropoffPoint);
        if(furnaces.size() > 0)
        	transferItemsToFurnace(storageMinecart, dropoffPoint);
        if(dispensers.size() > 0)
        	transferItemsToDispenser(storageMinecart,dropoffPoint);
        }
    }

    private boolean isValidStorageMinecart(Entity entity, Location location) {
        if(!(entity instanceof StorageMinecart)) 
        {
        	//System.out.println(entity.getEntityId());
            return false;
        }

        StorageMinecart storageMinecart=(StorageMinecart) entity;
        double distanceToDetectorRail=storageMinecart.getLocation().distance(location);

        //System.out.println(distanceToDetectorRail);
        return distanceToDetectorRail <= 1.5;
    }

    private void transferItemsToDropoffPoint(StorageMinecart storageMinecart, DropoffPoint dropoffPoint) {
        Inventory minecartInventory=storageMinecart.getInventory();

        List<Location> chestLocations=dropoffPoint.getChestLocations();
        List<Chest> dropoffChests=new ArrayList<Chest>();

        for(Location location:chestLocations) {
        	if(location.getBlock().getType() == Material.CHEST)
        	{
        		dropoffChests.add((Chest) location.getBlock().getState());
        	}
        }

		//System.out.println(dropoffChests.size());
        ItemStack[] minecartItems=minecartInventory.getContents();

        if(minecartItems!=null) {
            for(int i=0; i<minecartItems.length; i++) {
                ItemStack item=minecartItems[i];
                if(item!=null && item.getAmount()>0) {
                    addToAvailableDropoffChest(i, minecartInventory, item, dropoffChests);
                }
            }
        }

        // update state of all chests
        for(Chest chest:dropoffChests) {
            chest.update();
        }
    }
    
    private void transferItemsToDispenser(StorageMinecart storageMinecart, DropoffPoint dropoffPoint) {
        Inventory minecartInventory=storageMinecart.getInventory();

        List<Location> chestLocations=dropoffPoint.getChestLocations();
        List<Dispenser> dropoffChests=new ArrayList<Dispenser>();

        for(Location location:chestLocations) {
        	if(location.getBlock().getType() == Material.DISPENSER)
        	{
        		dropoffChests.add((Dispenser) location.getBlock().getState());
        	}
        }

		//System.out.println(dropoffChests.size());
        ItemStack[] minecartItems=minecartInventory.getContents();

        if(minecartItems!=null) {
            for(int i=0; i<minecartItems.length; i++) {
                ItemStack item=minecartItems[i];
                if(item!=null && item.getAmount()>0) {
                    addToAvailableDispenser(i, minecartInventory, item, dropoffChests);
                }
            }
        }

        // update state of all chests
        for(Dispenser chest:dropoffChests) {
            chest.update();
        }
    }
    
    private void transferItemsToFurnace(StorageMinecart storageMinecart, DropoffPoint dropoffPoint) {
        Inventory minecartInventory=storageMinecart.getInventory();

        List<Location> chestLocations=dropoffPoint.getChestLocations();
        List<Furnace> dropoffChests=new ArrayList<Furnace>();

        for(Location location:chestLocations) 
        {
        	if(location.getBlock().getType() == Material.FURNACE || location.getBlock().getType() == Material.BURNING_FURNACE)
        		dropoffChests.add((Furnace)location.getBlock().getState());
        }

        ItemStack[] minecartItems=minecartInventory.getContents();

        if(minecartItems!=null) {
            for(int i=0; i<minecartItems.length; i++) {
                ItemStack item=minecartItems[i];
                if(item!=null && item.getAmount()>0) {
                    addToAvailableFurnace(i, minecartInventory, item, dropoffChests);
                }
            }
        }

        // update state of all chests
        for(Furnace chest:dropoffChests) {
            chest.update();
        }
    }
    
    private void transferItemsFromFurnace(StorageMinecart storageMinecart, DropoffPoint dropoffPoint) {
        Inventory minecartInventory=storageMinecart.getInventory();

        List<Location> chestLocations=dropoffPoint.getChestLocations();
        List<Furnace> dropoffChests=new ArrayList<Furnace>();

        for(Location location:chestLocations) 
        {
        	if(location.getBlock().getType() == Material.FURNACE || location.getBlock().getType() == Material.BURNING_FURNACE)
        		dropoffChests.add((Furnace)location.getBlock().getState());
        }

        List<Furnace> duplicateListOfChests=new ArrayList<Furnace>(dropoffChests);
        Iterator<Furnace> iterator=duplicateListOfChests.iterator();

        if(minecartInventory != null)
        {
        while(iterator.hasNext()) 
        {
            Furnace chest=iterator.next();
            ItemStack[] chestItems = chest.getInventory().getContents();
            Inventory chestInventory = chest.getInventory();
            //System.out.println("loop");
            Map<Integer, ItemStack> excess=new HashMap<Integer, ItemStack>();
            ItemStack item = chestItems[2];
            if(item!=null && item.getAmount() > 0) 
            {
            	excess.putAll(minecartInventory.addItem(item));
            	if(excess.size()>0) 
            	{
            		for(Integer key:excess.keySet()) 
            		{
            			ItemStack excessItem=excess.get(key);
            			item.setAmount(excessItem.getAmount());
            		}

            		excess.clear();
            		iterator.remove();
            	} 
            	else 
            	{
            		//System.out.println("null");
            		item=null;
            		//continue;
            	}
            	chestInventory.setItem(2, item);
            	//takeFromAvailableDropoffChest(i, minecartInventory, item, dropoffChests);
            }

        }

        // update state of all chests
        for(Furnace chest:dropoffChests) {
            chest.update();
        }
        
        }
    }
    
    private void transferItemsFromDropoffPoint(StorageMinecart storageMinecart, DropoffPoint dropoffPoint) {
        Inventory minecartInventory=storageMinecart.getInventory();

        List<Location> chestLocations=dropoffPoint.getChestLocations();
        List<Chest> dropoffChests=new ArrayList<Chest>();

        for(Location location:chestLocations) {
            dropoffChests.add((Chest)location.getBlock().getState());
        }

        List<Chest> duplicateListOfChests=new ArrayList<Chest>(dropoffChests);
        Iterator<Chest> iterator=duplicateListOfChests.iterator();

        /*if(minecartItems!=null) 
        {
            for(int i=0; i< minecartItems.length; i++) 
            {
                ItemStack item = minecartItems[i];
                if(item!=null && item.getAmount()>0) 
                {
                    takeFromAvailableDropoffChest(i, minecartInventory, item, dropoffChests);
                }
            }
        }*/
        if(minecartInventory != null)
        {
        while(iterator.hasNext()) 
        {
            Chest chest=iterator.next();
            ItemStack[] chestItems = chest.getInventory().getContents();
            Inventory chestInventory = chest.getInventory();
            for(int i=0; i < chestItems.length; i++) 
            {
            	//System.out.println("loop");
                Map<Integer, ItemStack> excess=new HashMap<Integer, ItemStack>();
                ItemStack item = chestItems[i];
                if(item!=null && item.getAmount() > 0) 
                {
                    excess.putAll(minecartInventory.addItem(item));
                	if(excess.size()>0) 
                    {
                        for(Integer key:excess.keySet()) 
                        {
                            ItemStack excessItem=excess.get(key);
                            item.setAmount(excessItem.getAmount());
                        }

                        excess.clear();
                        iterator.remove();
                    } 
                    else 
                    {
                    	//System.out.println("null");
                        item=null;
                        //continue;
                    }
                	chestInventory.setItem(i, item);
                    //takeFromAvailableDropoffChest(i, minecartInventory, item, dropoffChests);
                }
            }
            
        }

        // update state of all chests
        for(Chest chest:dropoffChests) {
            chest.update();
        }
        
        }
    }

    private void addToAvailableDropoffChest(int inventoryIndex, Inventory minecartInventory, ItemStack item, List<Chest> dropoffChests) {
        List<Chest> duplicateListOfChests=new ArrayList<Chest>(dropoffChests);

        Iterator<Chest> iterator=duplicateListOfChests.iterator();
        Map<Integer, ItemStack> excess=new HashMap<Integer, ItemStack>();

        while(iterator.hasNext()) {
            Chest chest=iterator.next();

            excess.putAll(chest.getInventory().addItem(item));

            if(excess.size()>0) 
            {
                for(Integer key:excess.keySet()) 
                {
                    ItemStack excessItem=excess.get(key);
                    item.setAmount(excessItem.getAmount());
                }

                excess.clear();
                iterator.remove();
            } 
            else 
            {
                item=null;
                break;
            }
        }

        minecartInventory.setItem(inventoryIndex, item);
    }
    
    private void addToAvailableDispenser(int inventoryIndex, Inventory minecartInventory, ItemStack item, List<Dispenser> dropoffChests) {
        List<Dispenser> duplicateListOfChests=new ArrayList<Dispenser>(dropoffChests);

        Iterator<Dispenser> iterator=duplicateListOfChests.iterator();
        Map<Integer, ItemStack> excess=new HashMap<Integer, ItemStack>();

        while(iterator.hasNext()) {
            Dispenser chest=iterator.next();

            excess.putAll(chest.getInventory().addItem(item));

            if(excess.size()>0) 
            {
                for(Integer key:excess.keySet()) 
                {
                    ItemStack excessItem=excess.get(key);
                    item.setAmount(excessItem.getAmount());
                }

                excess.clear();
                iterator.remove();
            } 
            else 
            {
                item=null;
                break;
            }
        }

        minecartInventory.setItem(inventoryIndex, item);
    }
    
    private void addToAvailableFurnace(int inventoryIndex, Inventory minecartInventory, ItemStack item, List<Furnace> dropoffChests) {
        List<Furnace> duplicateListOfChests=new ArrayList<Furnace>(dropoffChests);

        Material[] burnable = new Material[] { Material.COAL, Material.WOOD, Material.SAPLING, Material.STICK, Material.FENCE, Material.WOOD_STAIRS, Material.TRAP_DOOR, Material.WORKBENCH, Material.BOOKSHELF, Material.CHEST, Material.JUKEBOX, Material.NOTE_BLOCK, Material.HUGE_MUSHROOM_1, Material.HUGE_MUSHROOM_2, Material.BLAZE_ROD, Material.LAVA_BUCKET };
        Iterator<Furnace> iterator=duplicateListOfChests.iterator();
        //Map<Integer, ItemStack> excess=new HashMap<Integer, ItemStack>();

        while(iterator.hasNext()) {
            Furnace chest=iterator.next();

            if(chest == null)
            	continue;
            //If burnable
            for(Material m : burnable)
            {
            	if(item.getType() == m)
            	{
            		if(chest.getInventory().getFuel() == null)
            		{
            			chest.getInventory().setItem(1, item);
            			item = null;
            			break;
            		}
            		else if(chest.getInventory().getFuel().getType() == item.getType())
            		{
            			if(chest.getInventory().getFuel().getAmount() + item.getAmount() <= 64)
            			{
            				item.setAmount(chest.getInventory().getFuel().getAmount() + item.getAmount());
            				chest.getInventory().setItem(1, item);
                    		item = null;
                    		break;
            			}
            		}
            	}
            }
            //If it's empty
        	if(chest.getInventory().getSmelting() == null)
        	{
        		chest.getInventory().setItem(0, item);
        		item = null;
        		break;
        	}
        	//If full of same item
            if(item == null)
            	break;
        	if(chest.getInventory().getSmelting().getType() == item.getType())
    		{
            	if(chest.getInventory().getSmelting().getAmount() + item.getAmount() <= 64)
            	{
            		item.setAmount(chest.getInventory().getSmelting().getAmount() + item.getAmount());
            		chest.getInventory().setItem(0, item);
            		item = null;
            		break;
            	}
    		}
        }
        		minecartInventory.setItem(inventoryIndex, item);
    }
}
