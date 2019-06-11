package buildcraft.additionalpipes.gui;

import java.util.stream.IntStream;

import org.lwjgl.opengl.GL11;

import buildcraft.additionalpipes.network.PacketHandler;
import buildcraft.additionalpipes.network.message.MessageDistPipe;
import buildcraft.additionalpipes.network.message.MessageDistPipeFluids;
import buildcraft.additionalpipes.pipes.PipeBehaviorDistribution;
import buildcraft.additionalpipes.pipes.PipeBehaviorDistributionFluids;
import buildcraft.additionalpipes.textures.Textures;
import buildcraft.additionalpipes.utils.Log;
import buildcraft.additionalpipes.utils.NetworkUtils;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import scala.actors.threadpool.Arrays;

@SideOnly(Side.CLIENT)
public class GuiDistributionPipeFluids extends GuiContainer {

	protected int xSize;
	protected int ySize;
	private GuiButton[] buttons = new GuiButton[19];
	private int[] numButtons = new int[6];
	public int guiX = 0;
	public int guiY = 0;
	private final PipeBehaviorDistributionFluids pipe;
	private int[] guiData = null;

	public GuiDistributionPipeFluids(PipeBehaviorDistributionFluids pipe) {
		super(new ContainerDistributionPipeFluids(pipe));
		this.pipe = pipe;
		guiData = new int[pipe.distData.length];
		System.arraycopy(pipe.distData, 0, guiData, 0, pipe.distData.length);
		xSize = 117;
		ySize = 150;
	}

	@Override
	public void initGui() {
		super.initGui();
		pipe.gui = this;
		System.arraycopy(pipe.distData, 0, guiData, 0, pipe.distData.length);
		// int bw = this.xSize - 20;
		int guiX = (width - xSize) / 2 + 30;
		int guiY = (height - ySize) / 2 - 7;

		buttonList.add(buttons[0] = new GuiButton(1, guiX + 1, guiY + 24, 20, 17, "-"));
		buttonList.add(buttons[1] = new GuiButton(2, guiX + 3 + 20, guiY + 24, 30, 17, "0"));
		numButtons[0] = buttons[1].id;
		buttonList.add(buttons[2] = new GuiButton(3, guiX + 5 + 50, guiY + 24, 20, 17, "+"));

		buttonList.add(buttons[3] = new GuiButton(4, guiX + 1, guiY + 25 + 17, 20, 17, "-"));
		buttonList.add(buttons[4] = new GuiButton(5, guiX + 3 + 20, guiY + 25 + 17, 30, 17, "0"));
		numButtons[1] = buttons[4].id;
		buttonList.add(buttons[5] = new GuiButton(6, guiX + 5 + 50, guiY + 25 + 17, 20, 17, "+"));

		buttonList.add(buttons[6] = new GuiButton(7, guiX + 1, guiY + 26 + 17 * 2, 20, 17, "-"));
		buttonList.add(buttons[7] = new GuiButton(8, guiX + 3 + 20, guiY + 26 + 17 * 2, 30, 17, "0"));
		numButtons[2] = buttons[7].id;
		buttonList.add(buttons[8] = new GuiButton(9, guiX + 5 + 50, guiY + 26 + 17 * 2, 20, 17, "+"));

		buttonList.add(buttons[9] = new GuiButton(10, guiX + 1, guiY + 27 + 17 * 3, 20, 17, "-"));
		buttonList.add(buttons[10] = new GuiButton(11, guiX + 3 + 20, guiY + 27 + 17 * 3, 30, 17, "0"));
		numButtons[3] = buttons[10].id;
		buttonList.add(buttons[11] = new GuiButton(12, guiX + 5 + 50, guiY + 27 + 17 * 3, 20, 17, "+"));

		buttonList.add(buttons[12] = new GuiButton(13, guiX + 1, guiY + 28 + 17 * 4, 20, 17, "-"));
		buttonList.add(buttons[13] = new GuiButton(14, guiX + 3 + 20, guiY + 28 + 17 * 4, 30, 17, "0"));
		numButtons[4] = buttons[13].id;
		buttonList.add(buttons[14] = new GuiButton(15, guiX + 5 + 50, guiY + 28 + 17 * 4, 20, 17, "+"));

		buttonList.add(buttons[15] = new GuiButton(16, guiX + 1, guiY + 29 + 17 * 5, 20, 17, "-"));
		buttonList.add(buttons[16] = new GuiButton(17, guiX + 3 + 20, guiY + 29 + 17 * 5, 30, 17, "0"));
		numButtons[5] = buttons[16].id;
		buttonList.add(buttons[17] = new GuiButton(18, guiX + 5 + 50, guiY + 29 + 17 * 5, 20, 17, "+"));
		
		buttonList.add(buttons[18] = new GuiButton(19, guiX + 7, guiY + 30 + 17 * 6, 45, 17, "APPLY"));
		buttons[18].enabled = false; // same as pipe data
		
		Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Please take care to set 0 for the side(s) which will be getting fluid."));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int p1, int p2) 
	{			
		buttons[1].displayString = getRep(guiData[0]);
		buttons[4].displayString = getRep(guiData[1]);
		buttons[7].displayString = getRep(guiData[2]);
		buttons[10].displayString = getRep(guiData[3]);
		buttons[13].displayString = getRep(guiData[4]);
		buttons[16].displayString = getRep(guiData[5]);
		
		for (int i = 0; i < numButtons.length; i++)
		{
			if (guiData[i] <= -1)
			{
				buttons[i * 3].enabled = false;
				buttons[(i * 3) + 1].enabled = false;
				buttons[(i * 3) + 2].enabled = false;
			}
			else if (guiData[i] == 0)
			{
				buttons[i * 3].enabled = false;
				buttons[(i * 3) + 1].enabled = true;
				buttons[(i * 3) + 2].enabled = true;
			}
			else
			{
				buttons[i * 3].enabled = true;
				buttons[(i * 3) + 1].enabled = true;
				buttons[(i * 3) + 2].enabled = true;
			}
		}
		
		int[] tmp = new int[guiData.length];
		System.arraycopy(guiData, 0, tmp, 0, guiData.length);
		if (Arrays.equals(guiData, pipe.distData) || !pipe.isSane(tmp))
		{
			buttons[18].enabled = false;
		}
		else
		{
			buttons[18].enabled = true;
		}
		
		fontRenderer.drawString(I18n.format("gui.distribution_pipe_fluids.title"), guiX + 34, guiY + 14, 4210752);
	}
	
	@Override
	public void onGuiClosed()
    {
        pipe.gui = null;
		
		super.onGuiClosed();
    }
	
	protected String getRep(int data)
	{
		return data != -1 ? (data != -69 ? java.lang.Integer.toString(data) : "NC") : "REM";
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) 
	{	
		int index = (guibutton.id - 1) / 3;
		int newData = guibutton.id != 19 ? guiData[index] : -70;
		
		if (guibutton.id == 19) 
		{
			apply();
			return;
		}
		else if (IntStream.of(numButtons).anyMatch(x -> x == guibutton.id))
		{
			if (guiData[index] != -1)
			{			
				for (int i = 0; i < numButtons.length; i++)
				{
					if (guiData[i] == -1)
					{	
						guiData[i] = pipe.distData[i] != -1 ? pipe.distData[i] : 0;
						break;
					}
				}
				
				newData = -1;
			}
		}
		else if ((guibutton.id - 1) % 3 == 0)
		{
			if (newData > 0)
			{
				newData--;
			}
		}
		else
		{
			newData++;
		}
		
		if(newData < -1 && newData != -69)
		{
			return;
		}
		
		guiData[index] = newData;
	}
	
	protected void apply()
	{
		//guiData = pipe.sanityCheck(guiData);
		
		// save data and send packet
		System.arraycopy(guiData, 0, pipe.distData, 0, guiData.length);
		int[] tmp = new int[guiData.length];
		System.arraycopy(guiData, 0, tmp, 0, guiData.length);
		MessageDistPipeFluids message = new MessageDistPipeFluids(pipe.getPos(), tmp);
		PacketHandler.INSTANCE.sendToServer(message);
		pipe.writeToNbt();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(Textures.GUI_DISTRIBUTION_FLUID);
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		drawTexturedModalRect(j, k, 0, 0, xSize, ySize);

	}
	
	public void forceData()
	{
		System.arraycopy(pipe.distData, 0, guiData, 0, pipe.distData.length);
	}
}
