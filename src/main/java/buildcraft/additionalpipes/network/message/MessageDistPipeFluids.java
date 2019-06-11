package buildcraft.additionalpipes.network.message;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import buildcraft.additionalpipes.pipes.PipeBehaviorDistributionFluids;
import buildcraft.additionalpipes.utils.Log;
import buildcraft.transport.tile.TilePipeHolder;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import scala.actors.threadpool.Arrays;

/**
 * Message that sets the properties of a Distribution Pipe from the GUI
 *
 */
public class MessageDistPipeFluids implements IMessage, IMessageHandler<MessageDistPipeFluids, IMessage>
{
	public BlockPos position;
	int[] _newData = null;
	
    public MessageDistPipeFluids()
    {
    }

    public MessageDistPipeFluids(BlockPos position, int[] newData)
    {
    	this.position = position;
    	_newData = new int[newData.length];
    	System.arraycopy(newData, 0, _newData, 0, newData.length);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
    	position = BlockPos.fromLong(buf.readLong());
    	byte[] tmp = new byte[24]; // ugh
    	buf.readBytes(tmp);
    	int[] intArr = convert(tmp);
    	_newData = new int[tmp.length];
        _newData = intArr;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
    	buf.writeLong(position.toLong());
    	ByteBuffer byteBuffer = ByteBuffer.allocate(_newData.length * 4);        
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(_newData);
        
        byte[] tmp = byteBuffer.array();
        buf.writeBytes(tmp);
    }

    @Override
    public IMessage onMessage(MessageDistPipeFluids message, MessageContext ctx)
    {   	
    	World world = ctx.getServerHandler().player.getEntityWorld();
    	TileEntity te = world.getTileEntity(message.position);
		if (te instanceof TilePipeHolder)
		{
			PipeBehaviorDistributionFluids pipe = (PipeBehaviorDistributionFluids) ((TilePipeHolder) te).getPipe().getBehaviour();

			if (message._newData.length == pipe.distData.length) 
			{
				System.arraycopy(message._newData, 0, pipe.distData, 0, message._newData.length);
			}
		}
    	
    	return null;
    }

    @Override
    public String toString()
    {
        return "MessageDistPipeFluids";
    }
    
    private int[] convert(byte buf[]) {
	   int intArr[] = new int[buf.length / 4];
	   int offset = 0;
	   
	   for(int i = 0; i < intArr.length; i++) {
	      intArr[i] = (buf[3 + offset] & 0xFF) | ((buf[2 + offset] & 0xFF) << 8) |
	                  ((buf[1 + offset] & 0xFF) << 16) | ((buf[0 + offset] & 0xFF) << 24);  
	      offset += 4;
	   }
	   
	   return intArr;
	}
}
