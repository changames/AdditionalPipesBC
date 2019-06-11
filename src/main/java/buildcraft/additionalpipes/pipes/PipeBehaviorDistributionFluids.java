/**
 * BuildCraft is open-source. It is distributed under the terms of the
 * BuildCraft Open Source License. It grants rights to read, modify, compile
 * or run the code. It does *NOT* grant the right to redistribute this software
 * or its modifications in any form, binary or source, except if expressively
 * granted by the copyright holder.
 */

package buildcraft.additionalpipes.pipes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

import buildcraft.additionalpipes.APConfiguration;
import buildcraft.additionalpipes.AdditionalPipes;
import buildcraft.additionalpipes.gui.GuiDistributionPipeFluids;
import buildcraft.additionalpipes.gui.GuiHandler;
import buildcraft.additionalpipes.utils.Log;
import buildcraft.additionalpipes.utils.NetworkUtils;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEvent;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEventItem.ItemEntry;
import buildcraft.lib.misc.EntityUtil;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import ibxm.Player;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;


public class PipeBehaviorDistributionFluids extends APPipe {
	public int distData[] = { -69, -69, -69, -69, -69, -69 };
	
	public GuiDistributionPipeFluids gui = null;
	
	private boolean halt = false;
	private boolean halted = false;

	public PipeBehaviorDistributionFluids(IPipe pipe, NBTTagCompound nbt)
	{
		super(pipe, nbt);
		
		for (int i = 0; i < distData.length; i++) {
			distData[i] = nbt.getInteger("distData" + i);
		}
		
		sanityCheck();
	}

	public PipeBehaviorDistributionFluids(IPipe pipe)
	{
		super(pipe);
	}

	@Override
	public int getTextureIndex(EnumFacing connection)
	{
		if (connection == null)
		{
			return EnumFacing.EAST.ordinal();
		}
		
		return connection.ordinal();
	}
	
	/*@PipeEventHandler
	public void onConnectionChange(PipeEventConnectionChange event) // bc8 when 
	{
		
	}*/
	
	@Override
	public void onTick() // gross
	{	
		boolean changed = false;
		
		int count = 0;
		for (EnumFacing side : EnumFacing.VALUES)
		{
			boolean check = false;
			try
			{
				check = pipe.getConnectedType(side) != ConnectedType.PIPE || (pipe.getConnectedType(side) == ConnectedType.PIPE && !canConnect(side, pipe.getConnectedPipe(side).getBehaviour()));
			}
			catch (Exception e)
			{ return; }

            if (check)
            {
            	if (distData[count] != -69)
            	{
                	halt = true;
                	changed = true;
                	
                	int sanity = 0;
            		while (!halted)
            		{
            			if (sanity >= 10)
            			{
            				break;
            			}
            			
            			sanity++;
            			try { Thread.sleep(10); } catch (InterruptedException e) {}
            		}
                	
                	distData[count] = -69;
            	}
            }
            else
            {
            	if (distData[count] == -69)
            	{
            		halt = true;
            		changed = true;
            		
            		int sanity = 0;
            		while (!halted)
            		{
            			if (sanity >= 10)
            			{
            				break;
            			}
            			
            			sanity++;
            			try { Thread.sleep(10); } catch (InterruptedException e) {}
            		}
            		
            		distData[count] = 0;
            	}
            }
            
            count++;
		}
		
		if (changed)
		{
			sanityCheck();
		}
	}
	
	@PipeEventHandler
    public void fluidInsert(PipeEventFluid.TryInsert insert) {
		if (distData[insert.from.ordinal() % distData.length] == -1 || distData[insert.from.ordinal() % distData.length] > 0)
		{
			Log.info("blocked " + insert.from.toString());
			insert.cancel();
		}
    }
	
	@PipeEventHandler
	public void preMoveCenter(PipeEventFluid.PreMoveToCentre event)
	{
		int count = 0;
		for(EnumFacing side : EnumFacing.VALUES)
		{
			if (distData[count] == -1 || distData[count] > 0)
			{
				Log.info("blocked " + side.toString());
				event.actuallyOffered[side.ordinal()] = 0;
			}
			count++;
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@PipeEventHandler
	public void onMoveCenter(PipeEventFluid.OnMoveToCentre event)
	{		
		ArrayList<PipeBehaviour> connectedPipes = new ArrayList();
		
		for (EnumFacing face : EnumFacing.VALUES) 
        {
			ConnectedType type = pipe.getConnectedType(face);
            if (type == ConnectedType.PIPE) 
            {
            	PipeBehaviour conpipe = pipe.getConnectedPipe(face).getBehaviour();
            	
            	connectedPipes.add(conpipe);
            }
            else
            {
            	connectedPipes.add(null);
            }
        }
		
		Log.debug("[FluidDistributionPipe] Got " + event.fluid.amount + " mB of fluid");
		
		FluidStack remaining = event.fluid.copy();
					
		int donewith = 0;
		// loop until we're out of fluid, or until no pipes need it
		while(remaining.amount > 0 && donewith < connectedPipes.size() - 1)
		{
			if (halt)
			{
				halted = true;
				continue;
			}
			halted = false;
			
			int[] realDist = new int[distData.length];
			System.arraycopy(distData, 0, realDist, 0, distData.length);
			
			int rem = -1;
			for (int i = 0; i < realDist.length; i++) 
			{
				if (realDist[i] == -1)
				{
					rem = i;
					continue;
				}
				
				if (distData[i] < 1)
				{
					continue;
				}
				
				realDist[i] = remaining.amount / distData[i];
			}
			
			// insert one allocation into each pipe that needs it
			Iterator<PipeBehaviour> pipeIter = connectedPipes.iterator();
			int count = 0;
			
			while (pipeIter.hasNext() && count < realDist.length)
			{
				PipeBehaviour pipe = pipeIter.next();
				
				if (count == rem || pipe == null)
				{
					if (pipe == null)
					{
						donewith++;
					}
					
					count++;
					continue;
				}
				
				FluidStack toInsert = remaining.copy();
				toInsert.amount = realDist[count];
				int inserted = ((PipeFlowFluids) pipe.pipe.getFlow()).insertFluidsForce(toInsert, EnumFacing.VALUES[count].getOpposite(), false);

				if (inserted == 0)
				{
					donewith++;
				}
				
				remaining.amount -= inserted;
				
				count++;
			}
			
			if (rem < 0)
			{
				continue;
			}
			
			if (connectedPipes.get(rem) == null || (connectedPipes.get(rem) != null && connectedPipes.get(rem).pipe == null))
			{
				continue;
			}
			
			int inserted = ((PipeFlowFluids) connectedPipes.get(rem).pipe.getFlow()).insertFluidsForce(remaining, EnumFacing.VALUES[rem].getOpposite(), false);
			remaining.amount -= inserted;
		}
		
		// update event data
		for(EnumFacing side : EnumFacing.VALUES)
		{		
			event.fluidEnteringCentre[side.ordinal()] = 0;
			
			if(remaining.amount > 0)
			{
				// decrease the amount of fluid entering the side to match what was actually consumed
				int fluidBlockedFromEntering = Math.min(event.fluidLeavingSide[side.ordinal()], remaining.amount);
				
				event.fluidLeavingSide[side.ordinal()] -= fluidBlockedFromEntering;
				remaining.amount -= fluidBlockedFromEntering;
			}
		}
	}

	@Override
    public boolean onPipeActivate(EntityPlayer player, RayTraceResult trace, float hitX, float hitY, float hitZ, EnumPipePart part) 
	{
        if (EntityUtil.getWrenchHand(player) != null) 
        {
            return super.onPipeActivate(player, trace, hitX, hitY, hitZ, part);
        }
        
        if(!player.world.isRemote) 
        {
        	// fire off an update packet to the client
        	pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
        	
        	BlockPos pipePos = pipe.getHolder().getPipePos();
        	player.openGui(AdditionalPipes.instance, GuiHandler.PIPE_DIST_FLUID, pipe.getHolder().getPipeWorld(), pipePos.getX(), pipePos.getY(), pipePos.getZ());
        }
        
        return true;
    }
	
	public void sanityCheck()
	{
		halt = true;
		
		int sanity = 0;
		while (!halted)
		{
			if (sanity >= 10)
			{
				break;
			}
			
			sanity++;
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
		
		int[] distCache = new int[distData.length];
		System.arraycopy(distData, 0, distCache, 0, distData.length);
		distData = sanityCheck(distData);
		
		if (!Arrays.equals(distData, distCache) && gui != null)
		{
			gui.forceData();
		}
		
		halt = false;
	}
	
	public boolean isSane(int[] data)
	{
		int[] tmp = new int[data.length];
		System.arraycopy(data, 0, tmp, 0, data.length);
		
		return Arrays.equals(sanityCheck(data), tmp);
	}

	public int[] sanityCheck(int[] data) // EWWWWWWWWW
	{
		Map<Integer, Long> map = Arrays.stream(data).boxed()       
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		
		Long ncCount = map.get(-69);
		if (ncCount != null && ncCount == data.length)
		{
			return data;
		}
		else
		{
			Long tmp = map.get(-1);
			if (tmp != null && tmp == 1)
			{
				tmp = map.get(0);
				if (tmp == null || (tmp != null && tmp < (data.length - (ncCount + 1))))
				{
					if (check(data))
					{
						return data;
					}
				}
			}
		}

		
		for (int i = 0; i < data.length; i++) {
			map = Arrays.stream(data).boxed()       
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			
			if (data[i] == -69)
			{
				continue;
			}
			
			Long tmp = map.get(-1);
			if (tmp == null)
			{
				data[i] = -1;
				continue;
			}
			else if (tmp != null && tmp > 1)
			{
				if (data[i] == -1)
				{
					data[i] = 0;
					continue;
				}
			}
			
			if (data[i] == -1)
			{
				continue;
			}
			
			tmp = map.get(1);
			if (tmp == null)
			{
				data[i] = 1;
				continue;
			}
			else if (tmp != null && tmp > 1)
			{
				if (data[i] == 1)
				{
					data[i] = 0;
					continue;
				}
			}
			
			if (data[i] == 1)
			{
				continue;
			}
			
			data[i] = 0;
		}
		
		if (!check(data))
		{
			return sanityCheck(data);
		}
			
		return data;
	}
	
	public boolean check(int[] arr)
	{
		int[] tmpArr = null;
		tmpArr = new int[arr.length];
		System.arraycopy(arr, 0, tmpArr, 0, arr.length);
		int lcm = NetworkUtils.lcm(tmpArr);
		int res = 0;
		
		for (int data : arr)
		{
			if (data < 1)
			{
				continue;
			}
			
			res += (lcm / data);
		}
		
		if (res <= lcm) // output total is 1 or lower (without REM and NC)
		{
			return true;
		}
		
		return false;
	}

	@Override
	public NBTTagCompound writeToNbt() {
		NBTTagCompound nbt = super.writeToNbt();

		for (int i = 0; i < distData.length; i++) 
		{
			nbt.setInteger("distData" + i, distData[i]);
		}
		
		return nbt;
	}
	
	
    @Override
    public void writePayload(PacketBuffer buffer, Side side) 
    {
        super.writePayload(buffer, side);
        if (side == Side.SERVER) 
        {       	
        	for (int i = 0; i < distData.length; i++) 
    		{
        		buffer.writeInt(distData[i]);
    		}
        }
    }
    
    @Override
    public void readPayload(PacketBuffer buffer, Side side, MessageContext ctx) throws IOException
    {
        super.readPayload(buffer, side, ctx);
        if (side == Side.CLIENT) 
        {
            for (int i = 0; i < distData.length; i++) 
    		{
            	distData[i] = buffer.readInt();
    		}
        }
    }



}
