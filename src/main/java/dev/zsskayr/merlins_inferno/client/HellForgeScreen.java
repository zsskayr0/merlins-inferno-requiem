package dev.zsskayr.merlins_inferno.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.forge.ForgeClientConfig;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;
import dev.zsskayr.merlins_inferno.menu.HellForgeMenu;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** Native textured forge panels and a world-aware multiblock preview. */
public class HellForgeScreen extends AbstractContainerScreen<HellForgeMenu> {
    private boolean vaultPanel;
    private boolean tipHidden;
    private static final int DRAWER_W=148,DRAWER_H=168;
    private int drawerX() { return leftPos + imageWidth + DRAWER_W <= width ? imageWidth + 4 : imageWidth - DRAWER_W - 4; }
    private static final int ACCENT = 0xFFBD854E;
    private int selectedFace = -1;
    private float rotation;
    private float guiFill;
    private long lastFillNs;
    private float pitch=25F,panX,panY,zoom=1F;
    private boolean oriented;
    private net.minecraft.core.BlockPos previewCentre;
    private final float[][] portPositions = new float[dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity.MAX_PORTS][3];
    public HellForgeScreen(HellForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 352; imageHeight = 308;
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.merlins_inferno.forge." + key, args); }
    private void action(int id) { net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.zsskayr.merlins_inferno.network.ForgeActionPayload(menu.containerId, id)); }
    @Override protected void containerTick() { super.containerTick(); menu.scrollLanes(menu.firstLane()); }
    private static final net.minecraft.resources.ResourceLocation UI = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            dev.zsskayr.merlins_inferno.Merlins_inferno.MODID, "textures/gui/hellforge/hell_forge_ui.png");
    private static final net.minecraft.resources.ResourceLocation TITLES = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            dev.zsskayr.merlins_inferno.Merlins_inferno.MODID, "textures/gui/hellforge/titles.png");
    private static final int TEXT = 0xFFD6C5B0, TEXT_DIM = 0xFF9C8D80, TEXT_SHADOW = 0xFF0E0A0A, HOT = 0xFFFFB45A, HOT_SHADOW = 0xFF3A1D08;

    private void blit(GuiGraphics g,int u,int v,int w,int h,int x,int y){g.blit(UI,leftPos+x,topPos+y,u,v,w,h,256,256);}
    /** Repeats a texture region across an area, cropping the last tile. */
    private void tile(GuiGraphics g,int u,int v,int tw,int th,int x,int y,int w,int h){
        for(int yy=0;yy<h;yy+=th)for(int xx=0;xx<w;xx+=tw)blit(g,u,v,Math.min(tw,w-xx),Math.min(th,h-yy),x+xx,y+yy);
    }
    /** Nine-slice a square sprite: corners fixed, edges and centre tiled. */
    private void nine(GuiGraphics g,int u,int v,int sz,int b,int x,int y,int w,int h){
        int c=sz-2*b;
        tile(g,u+b,v+b,c,c,x+b,y+b,w-2*b,h-2*b);
        tile(g,u+b,v,c,b,x+b,y,w-2*b,b);tile(g,u+b,v+sz-b,c,b,x+b,y+h-b,w-2*b,b);
        tile(g,u,v+b,b,c,x,y+b,b,h-2*b);tile(g,u+sz-b,v+b,b,c,x+w-b,y+b,b,h-2*b);
        blit(g,u,v,b,b,x,y);blit(g,u+sz-b,v,b,b,x+w-b,y);blit(g,u,v+sz-b,b,b,x,y+h-b);blit(g,u+sz-b,v+sz-b,b,b,x+w-b,y+h-b);
    }
    private void panel(GuiGraphics g,int x,int y,int w,int h,int ignored){
        g.fill(leftPos+x+2,topPos+y+3,leftPos+x+w+2,topPos+y+h+3,0x50000000);
        nine(g,0,40,16,3,x,y,w,h);
    }
    private void well(GuiGraphics g,int x,int y,int w,int h,int ignored){nine(g,16,40,16,2,x,y,w,h);}
    private void slot(GuiGraphics g,int x,int y){blit(g,32,40,18,18,x-1,y-1);}
    /** Side tab: inset screen with a glowing icon (0 sort, 1 vaults, 5 C, 6 F, 7 K). */
    private void tab(GuiGraphics g,int x,int y,int icon,boolean active){
        blit(g,active?24:0,124,22,22,x,y);
        blit(g,icon*20,active?150:170,18,18,x+2,y+2);
    }
    private void label(GuiGraphics g,String value,int x,int y,int color){g.drawString(font,value,leftPos+x,topPos+y,color,false);}
    private void shadowLabel(GuiGraphics g,String value,int x,int y,int color,int shadow){
        g.drawString(font,value,leftPos+x+1,topPos+y+1,shadow,false);g.drawString(font,value,leftPos+x,topPos+y,color,false);
    }
    private void button(GuiGraphics g,int x,int y,int w,int h,String value,boolean active){
        nine(g,active?16:0,64,16,3,x,y,w,h);
        shadowLabel(g,value,x+(w-font.width(value))/2,y+(h-8)/2,active?0xFFFFE2B0:TEXT,active?HOT_SHADOW:TEXT_SHADOW);
    }
    /** Heated-metal bar: sunken trough with an ember gradient fill and a white-hot leading edge. */
    private void bar(GuiGraphics g,int x,int y,int w,int h,double value){bar(g,x,y,w,h,value,false);}
    /** The current fuel's colour (from its data), or a dim grey when the forge holds no fuel. */
    private int fuelColor(){
        int index=menu.value(7);
        var fuels=minecraft==null||minecraft.level==null?java.util.List.<ForgeThermalData.Fuel>of():ForgeThermalData.fuels(minecraft.level);
        if(menu.getStoredFuel()<=0||index<0||index>=fuels.size())return 0xFF6E6862;
        return 0xFF000000|fuels.get(index).uiColor();
    }
    /**
     * The gauge holds the fuel itself: its own animated liquid texture, tiled at full size, with a gently rolling surface.
     * Each one-pixel column is clipped to its own height so the top waves.
     */
    /** The fuel being drawn: the current one, or the last one while its liquid drains away after a dump. */
    private ForgeThermalData.Fuel shownFuel(){
        if(minecraft==null||minecraft.level==null)return null;
        int index=menu.value(7);
        var fuels=ForgeThermalData.fuels(minecraft.level);
        if(index>=0&&index<fuels.size())lastFuel=fuels.get(index);
        return lastFuel;
    }
    private ForgeThermalData.Fuel lastFuel;
    private void drawFuelLiquid(GuiGraphics g,int fill){
        if(fill<=0||minecraft==null||minecraft.level==null)return;
        var fuel=shownFuel();
        var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(net.minecraft.resources.ResourceLocation.parse(fuel==null?"minecraft:block/lava_still":fuel.liquidTexture()));
        int tint=fuel==null?0xFFFFFF:fuel.tint();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor((tint>>16&255)/255F,(tint>>8&255)/255F,(tint&255)/255F,1F);
        float time=minecraft.level.getGameTime()+minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        int left=leftPos+323,bottom=topPos+237;
        for(int col=0;col<10;col++){
            int wave=fill>=196?0:(int)Math.round(1.3*Math.sin(time*0.12+col*0.75)+0.6*Math.sin(time*0.07-col*1.3));
            int top=Math.max(topPos+40,bottom-Math.min(196,fill+wave));
            g.enableScissor(left+col,top,left+col+1,bottom);
            for(int ty=bottom-16;ty>top-16;ty-=16)g.blit(left,ty,0,16,16,sprite);
            g.disableScissor();
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F,1F,1F,1F);
        // a thin glint along the surface and the gauge's own side shading on top of the liquid
        for(int col=0;col<10;col++){
            int wave=fill>=196?0:(int)Math.round(1.3*Math.sin(time*0.12+col*0.75)+0.6*Math.sin(time*0.07-col*1.3));
            int top=Math.max(topPos+40,bottom-Math.min(196,fill+wave));
            g.fill(left+col,top,left+col+1,top+1,0x60FFFFFF);
        }
        g.fill(left,bottom-fill,left+1,bottom,0x30FFFFFF);g.fill(left+9,bottom-fill,left+10,bottom,0x40000000);
    }
    /**
     * The fuel's own three-stop ramp, built like the lava one: a paler tint turned a little up the hue wheel, the colour
     * itself, and a deeper shade turned a little down it (yellow, orange, red for lava), so every fuel glows in its own hue.
     */
    private static int fuelRamp(int color,double t,int shade){
        float[] hsv=java.awt.Color.RGBtoHSB(color>>16&255,color>>8&255,color&255,null);
        int light=net.minecraft.util.Mth.hsvToRgb((hsv[0]+0.055F)%1F,hsv[1]*0.62F,1F);
        int deep=net.minecraft.util.Mth.hsvToRgb((hsv[0]+0.945F)%1F,Math.min(1F,hsv[1]*1.1F),hsv[2]*0.82F);
        int[][] stops={{light>>16&255,light>>8&255,light&255},{color>>16&255,color>>8&255,color&255},{deep>>16&255,deep>>8&255,deep&255}};
        double f=Math.clamp(t,0,1)*2;int i=Math.min(1,(int)f);double k=f-i;
        int rr=(int)(stops[i][0]+(stops[i+1][0]-stops[i][0])*k),gg=(int)(stops[i][1]+(stops[i+1][1]-stops[i][1])*k),bb=(int)(stops[i][2]+(stops[i+1][2]-stops[i][2])*k);
        return 0xFF000000|Math.clamp(rr+shade,0,255)<<16|Math.clamp(gg+shade,0,255)<<8|Math.clamp(bb+shade,0,255);
    }
    /** The colour scaled by brightness, with a small per-row shade for texture. */
    private static int shade(int color,double brightness,int shade){
        int r=(int)((color>>16&255)*brightness)+shade,gr=(int)((color>>8&255)*brightness)+shade,b=(int)((color&255)*brightness)+shade;
        return 0xFF000000|Math.clamp(r,0,255)<<16|Math.clamp(gr,0,255)<<8|Math.clamp(b,0,255);
    }
    private static int heat(double t,int shade){
        int[][] stops={{255,226,96},{244,140,36},{206,44,24}};
        double f=Math.clamp(t,0,1)*2;int i=Math.min(1,(int)f);double k=f-i;
        int r=0,gr=0,b=0;
        r=(int)(stops[i][0]+(stops[i+1][0]-stops[i][0])*k);gr=(int)(stops[i][1]+(stops[i+1][1]-stops[i][1])*k);b=(int)(stops[i][2]+(stops[i+1][2]-stops[i][2])*k);
        return 0xFF000000|Math.clamp(r+shade,0,255)<<16|Math.clamp(gr+shade,0,255)<<8|Math.clamp(b+shade,0,255);
    }
    private void bar(GuiGraphics g,int x,int y,int w,int h,double value,boolean heat){
        nine(g,0,88,16,1,x,y,w,h);
        if(heat){
            int fillW=(int)((w-2)*Math.clamp(value,0,1));
            for(int i=0;i<fillW;i++){
                double t=(double)i/Math.max(1,w-3);
                int cx=leftPos+x+1+i;
                g.fill(cx,topPos+y+1,cx+1,topPos+y+h-1,heat(t,((i*7+x)%5)-2));
                g.fill(cx,topPos+y+1,cx+1,topPos+y+2,heat(t,36));
                g.fill(cx,topPos+y+h-2,cx+1,topPos+y+h-1,heat(t,-44));
            }
            if(fillW>1)g.fill(leftPos+x+fillW,topPos+y+1,leftPos+x+fillW+1,topPos+y+h-1,0xFFFFF0C8);
            return;
        }
        int fill=(int)((w-2)*Math.clamp(value,0,1));
        for(int xx=0;xx<fill;xx+=16){
            int seg=Math.min(16,fill-xx);
            g.blit(UI,leftPos+x+1+xx,topPos+y+1,seg,h-2,16,88,seg,7,256,256);
        }
        if(fill>1)g.fill(leftPos+x+fill,topPos+y+1,leftPos+x+fill+1,topPos+y+h-1,0xFFFFE8B0);
    }
    /** Outer frame after the reference: runic strips, dark gap and square corner blocks. */
    private void frame(GuiGraphics g,int x,int y,int w,int h){
        g.fill(leftPos+x+2,topPos+y+3,leftPos+x+w+2,topPos+y+h+3,0x60000000);
        tile(g,48,16,16,16,x+8,y+8,w-16,h-16);
        tile(g,0,16,16,8,x+8,y,w-16,8);tile(g,16,16,16,8,x+8,y+h-8,w-16,8);
        tile(g,32,16,8,16,x,y+8,8,h-16);tile(g,40,16,8,16,x+w-8,y+8,8,h-16);
        blit(g,0,0,8,8,x,y);blit(g,8,0,8,8,x+w-8,y);blit(g,16,0,8,8,x,y+h-8);blit(g,24,0,8,8,x+w-8,y+h-8);
    }
    private void vline(GuiGraphics g,int x,int y1,int y2){g.fill(leftPos+x,topPos+y1,leftPos+x+1,topPos+y2,0xFF161313);g.fill(leftPos+x+1,topPos+y1,leftPos+x+2,topPos+y2,0xFF4E4642);}
    private void hline(GuiGraphics g,int x1,int x2,int y){g.fill(leftPos+x1,topPos+y,leftPos+x2,topPos+y+1,0xFF161313);g.fill(leftPos+x1,topPos+y+1,leftPos+x2,topPos+y+2,0xFF4E4642);}
    /** Faint silhouette shown in an empty slot (0 flame, 1 dross, 2 scrap). */
    private void ghost(GuiGraphics g,int kind,int x,int y,int slotIndex){
        if(!menu.getSlot(slotIndex).hasItem())blit(g,kind*16,192,16,16,x,y);
    }
    @Override protected void renderBg(GuiGraphics g,float pt,int mx,int my){
        frame(g,24,8,328,300);
        // Title plate with angled shoulders around the baked lettering (tools/gen_forge_titles.py), tinted with the fuel's colour.
        boolean blighted = (menu.value(19) & 15) >= 2;
        int titleW = blighted ? 182 : 97;
        int plateW = Math.max(154, titleW + 22), plateX = 188 - plateW / 2;
        panel(g,plateX,0,plateW,26,0);
        for(int i=0;i<6;i++){
            g.fill(leftPos+plateX-6+i,topPos+10+i,leftPos+plateX,topPos+11+i,0xFF0E0A0A);
            g.fill(leftPos+plateX+plateW+6-i,topPos+10+i,leftPos+plateX+plateW,topPos+11+i,0xFF0E0A0A);
        }
        int titleColor = fuelColor();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor((titleColor >> 16 & 255) / 255F, (titleColor >> 8 & 255) / 255F, (titleColor & 255) / 255F, 1F);
        g.blit(TITLES, leftPos + 188 - titleW / 2, topPos + 6, 0, blighted ? 16 : 0, titleW, 15, 256, 32);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        int unit=ForgeClientConfig.UNIT.get()==ForgeClientConfig.Unit.KELVIN?7:ForgeClientConfig.UNIT.get()==ForgeClientConfig.Unit.CELSIUS?5:6;
        tab(g,1,57,0,menu.value(12)==1);
        tab(g,1,83,1,vaultPanel);
        tab(g,1,109,unit,false);

        // Furnace: 3D view, thermometer on the left, stats top-right.
        well(g,36,34,148,172,0);
        well(g,42,40,12,160,0);
        double heatFraction=Math.clamp((menu.value(2)-273.15)/Math.max(1,menu.value(9)-273.15),0,1);
        int tubeFill=(int)(156*heatFraction);
        for(int i=0;i<tubeFill;i++){
            int yy=topPos+198-i;double t=i/156.0;
            g.fill(leftPos+43,yy,leftPos+53,yy+1,heat(t,((i*7)%5)-2));
            g.fill(leftPos+43,yy,leftPos+44,yy+1,heat(t,30));g.fill(leftPos+52,yy,leftPos+53,yy+1,heat(t,-40));
        }
        for(int k=0;k<=10;k++){
            int ty=topPos+198-Math.round(k*15.6F);boolean major=k%5==0;
            g.fill(leftPos+55,ty,leftPos+(major?60:58),ty+1,major?0xFF9A8B7E:0xFF5C4F46);
        }
        label(g,ForgeClientConfig.temperature(menu.value(9)),63,38,TEXT_DIM);
        label(g,ForgeClientConfig.temperature(menu.value(8)),63,191,TEXT_DIM);
        String speed=Integer.toString(menu.value(5)),yield=Integer.toString(menu.value(6));
        label(g,"Speed",136,40,TEXT_DIM);label(g,speed,180-font.width(speed),40,TEXT);
        label(g,"Yield",136,52,TEXT_DIM);label(g,yield,180-font.width(yield),52,TEXT);
        well(g,104,186,76,14,0);
        String current=ForgeClientConfig.temperature(menu.value(2));
        shadowLabel(g,current,142-font.width(current)/2,189,HOT,HOT_SHADOW);
        g.enableScissor(leftPos+60,topPos+36,leftPos+182,topPos+184);
        renderPorts(g,leftPos+122,topPos+114);g.disableScissor();

        vline(g,190,34,292);
        hline(g,36,184,211);

        // A Magmatic forge: just the stone going in, the melt bar and the bucket being filled.
        if(menu.magmatic()){
            int y=38;
            slot(g,207,y+1);slot(g,289,y+1);
            bar(g,228,y+6,44,8,menu.progress(0));
            g.fill(leftPos+275,topPos+y+9,leftPos+280,topPos+y+11,0xFF7A6A5E);
            for(int k=0;k<4;k++)g.fill(leftPos+280+k,topPos+y+6+k,leftPos+281+k,topPos+y+14-k,0xFF7A6A5E);
        }
        // Smelting lanes: all nine fit, so there is nothing to scroll.
        for(int row=0;row<MENU_LANES&&!menu.magmatic();row++){
            int lane=menu.firstLane()+row,y=38+row*22;
            boolean open=lane<menu.getProcessingSlots();
            boolean stocked=menu.getSlot(HellForgeBlockEntity.inputSlot(lane)).hasItem()||menu.getSlot(HellForgeBlockEntity.outputSlot(lane)).hasItem();
            boolean running=open&&menu.progress(lane)>0;
            String n=Integer.toString(lane+1);
            shadowLabel(g,n,201-font.width(n)/2,y+6,running?HOT:open?TEXT_DIM:0xFF5C4F46,running?HOT_SHADOW:TEXT_SHADOW);
            if(open||stocked){slot(g,207,y+1);slot(g,289,y+1);}
            if(open){
                bar(g,228,y+6,44,8,menu.progress(lane));
                g.fill(leftPos+275,topPos+y+9,leftPos+280,topPos+y+11,0xFF7A6A5E);
                for(int k=0;k<4;k++)g.fill(leftPos+280+k,topPos+y+6+k,leftPos+281+k,topPos+y+14-k,0xFF7A6A5E);
            }
            if(row<MENU_LANES-1)hline(g,196,306,y+19);
        }
        vline(g,310,34,292);

        // Dross and scrap tray, centred under the lanes (a Magmatic forge leaves none).
        if(!menu.magmatic()){
            well(g,213,252,78,40,0);
            slot(g,229,264);slot(g,259,264);
            ghost(g,1,229,264,HellForgeBlockEntity.DROSS_SLOT);ghost(g,2,259,264,HellForgeBlockEntity.SCRAP_SLOT);
        }

        // Fuel column: tall reserve gauge, the fuel slot beneath it and the dump button last.
        well(g,321,38,14,200,0);
        // The gauge eases towards the real level like the pool does, so fuel pours in and drains out instead of jumping.
        int fuelFill=Math.round(196*Math.clamp(guiFill,0F,1F));
        drawFuelLiquid(g,fuelFill);
        for(int i=20;i<196;i+=20)g.fill(leftPos+323,topPos+236-i,leftPos+333,topPos+237-i,0x55000000);
        int capY=topPos+236-Math.round(196*menu.fillLimit());
        g.fill(leftPos+320,capY,leftPos+336,capY+1,0xFFFFE2B0);
        g.fill(leftPos+319,capY-1,leftPos+321,capY+2,0xFFFFE2B0);g.fill(leftPos+335,capY-1,leftPos+337,capY+2,0xFFFFE2B0);
        if(menu.redstoneLevel()>=0){int redY=topPos+236-Math.round(196*menu.effectiveFillLimit());g.fill(leftPos+322,redY,leftPos+334,redY+1,0xFFE04040);}
        slot(g,320,246);ghost(g,0,320,246,HellForgeBlockEntity.FUEL_SLOT);
        nine(g,0,64,16,3,317,273,22,18);
        blit(g,48,192,18,18,319,273);

        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(g,39+col*16,220+row*16);
        for(int col=0;col<9;col++)slot(g,39+col*16,274);
    }
    private static final int MENU_LANES=HellForgeMenu.LANE_ROWS;
    private boolean vaultPanelCovers(int x,int y){return vaultPanel&&isHovering(drawerX(),28,DRAWER_W,DRAWER_H,x,y);}
    private void renderDrawer(GuiGraphics g){
        int x=drawerX();
        frame(g,x,28,DRAWER_W,DRAWER_H);
        shadowLabel(g,"Vaults",x+11,42,HOT,HOT_SHADOW);
        button(g,x+121,38,16,14,"x",false);
        button(g,x+11,56,26,16,"<",false);button(g,x+111,56,26,16,">",false);
        well(g,x+40,56,68,16,0);
        String name=selectedFace<0?"None":"Vault "+(selectedFace+1);
        label(g,name,x+74-font.width(name)/2,60,selectedFace<0?TEXT_DIM:TEXT);
        int mode=selectedFace<0?-1:menu.value(65+selectedFace*3);
        for(int m=0;m<3;m++)button(g,x+11,78+m*18,126,16,new String[]{"Input","Output","Fuel"}[m],mode==m);
        boolean auto=selectedFace>=0&&menu.value(66+selectedFace*3)==1;
        button(g,x+11,136,126,16,"Auto: "+(auto?"ON":"OFF"),auto);
        label(g,"Slots",x+11,158,TEXT_DIM);
        for(int i=0;i<9;i++){
            boolean on=selectedFace>=0&&(menu.value(67+selectedFace*3)&(1<<i))!=0;
            button(g,x+11+i*14,168,13,16,Integer.toString(i+1),on);
        }
    }
    private void nextPort(int step){
        for(int n=1;n<=dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity.MAX_PORTS;n++){int i=Math.floorMod(selectedFace+step*n,dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity.MAX_PORTS);if(menu.value(65+i*3)>=0){selectedFace=i;return;}}
        selectedFace=-1;
    }
    private void renderPorts(GuiGraphics g, int cx, int cy) {
        if(!oriented && menu.value(10)>0) { rotation=Direction.from3DDataValue(menu.value(11)).toYRot()-45; oriented=true; }
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx + panX, cy + panY, 150);
        var shape = dev.zsskayr.merlins_inferno.multiblock.HellForgeStructure.Shape.unpack(menu.value(10));
        float scale = 64F * zoom / Math.max(shape.radius() * 2 + 1, shape.height());
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(0, -shape.centreY(), 0); // a tower turns around its middle, not its floor
        drawBlock(g, ModBlocks.DEMONIC_CORE.get().defaultBlockState(), 0, 0, 0);
        net.minecraft.core.BlockPos centre = new net.minecraft.core.BlockPos(
                (menu.value(13) & 65535) | (menu.value(14) << 16),
                (menu.value(15) & 65535) | (menu.value(16) << 16),
                (menu.value(17) & 65535) | (menu.value(18) << 16));
        previewCentre = centre;
        int r = shape.radius();
        int face = 0;
        for (int px = -r; px <= r; px++) for (int py = shape.yMin(); py <= shape.yMax(); py++) for (int pz = -r; pz <= r; pz++) {
            int axes = shape.axes(px, py, pz);
            if (axes == 0 || (px == 0 && py == 0 && pz == 0)) continue;
            boolean open = !shape.roof() && py == shape.yMax() && axes == 1;
            if (open) continue;
            BlockState block = ModBlocks.COMPRESSED_NETHERRACK.get().defaultBlockState();
            if (axes == 1) {
                int mode = menu.value(65 + face * 3);
                Vector3f projected = pose.last().pose().transformPosition(new Vector3f(px, py, pz));
                portPositions[face][0] = projected.x;
                portPositions[face][1] = projected.y;
                portPositions[face][2] = projected.z;
                block = mode >= 0 ? ModBlocks.HELL_FORGE_VAULT.get().defaultBlockState()
                        .setValue(HellForgeVaultBlock.MODE, HellForgeVaultBlock.Mode.values()[mode])
                        : ModBlocks.HELLGLASS.get().defaultBlockState();
                face++;
            }
            if (minecraft.level != null && minecraft.level.hasChunkAt(centre.offset(px,py,pz)))
                block = minecraft.level.getBlockState(centre.offset(px,py,pz));
            drawBlock(g, block, px, py, pz);
        }
        long nowNs = System.nanoTime();
        float dtFill = Math.min(1F, (nowNs - lastFillNs) / 1.0E9F);
        float targetFill = menu.getFuelCapacity() > 0 ? Math.min(1F, (float) menu.getStoredFuel() / menu.getFuelCapacity()) : 0F;
        guiFill = lastFillNs == 0 || dtFill >= 1F ? targetFill : guiFill + (targetFill - guiFill) * (1F - (float) Math.exp(-dtFill * 2.5F));
        lastFillNs = nowNs;
        if (r >= 2 && guiFill > 0.004F) {
            // Interior of the shell: blocks are centred on integer coordinates, so it spans +-(r - 0.5) minus one layer.
            float fill = guiFill;
            int depth = shape.height() - (shape.roof() ? 2 : 1);
            HellForgeFuelRenderer.draw(g.bufferSource(), pose.last(), -(r - 1) - 0.5F, (r - 1) + 0.5F, shape.yMin() + 0.5F,
                    -(r - 1) - 0.5F, (r - 1) + 0.5F, depth, fill, minecraft.level == null ? 0 : minecraft.level.getGameTime(),
                    shownFuel());
        }
        g.flush();
        pose.popPose();
        Lighting.setupFor3DItems();
        if (selectedFace >= 0 && menu.value(65 + selectedFace * 3) >= 0)
            g.renderOutline((int) portPositions[selectedFace][0] - 9, (int) portPositions[selectedFace][1] - 9, 18, 18, ACCENT);
    }
    private void drawBlock(GuiGraphics g, BlockState block, float x, float y, float z) {
        g.pose().pushPose();
        g.pose().translate(x - 0.5, y - 0.5, z - 0.5);
        var dispatcher=minecraft.getBlockRenderer();
        var modelData=net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
        if(previewCentre!=null && minecraft.level!=null) {
            var worldPos=previewCentre.offset((int)x,(int)y,(int)z);
            modelData=dispatcher.getBlockModel(block).getModelData(minecraft.level,worldPos,block,modelData);
        }
        dispatcher.renderSingleBlock(block,g.pose(),g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY,modelData,null);
        g.pose().popPose();
    }
    @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        var recipe = ForgeThermalData.recipe(minecraft.level, stack);
        if (recipe.isPresent()) {
            var material = ForgeThermalData.material(minecraft.level, stack, recipe.get().id());
            lines.add(text("range", ForgeClientConfig.temperature(material.min()), ForgeClientConfig.temperature(material.max())));
        }
        var fuel = ForgeThermalData.fuel(minecraft.level, stack);
        if (fuel != null) lines.add(text("range", ForgeClientConfig.temperature(fuel.min()), ForgeClientConfig.temperature(fuel.max())));
        return lines;
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {}
    /** Every material the forge knows, with its range and whether this forge, as it is now, can work it. */
    private List<Component> materialLines(){
        List<Component> lines=new ArrayList<>();
        lines.add(text("materials").copy().withStyle(net.minecraft.ChatFormatting.GOLD));
        int tier=menu.value(19)&15;double low=menu.value(8),high=menu.value(9);
        for(var entry:ForgeThermalData.materialTable(minecraft.level)){
            var m=entry.material();
            boolean ok=m.minCoreTier()<=tier&&m.min()<=high&&m.max()>=low;
            lines.add(Component.literal(ok?"\u2714 ":"\u2718 ").withStyle(ok?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED)
                    .append(entry.items().getFirst().getHoverName().copy().withStyle(net.minecraft.ChatFormatting.WHITE))
                    .append(Component.literal("  "+ForgeClientConfig.temperature(m.min())+" - "+ForgeClientConfig.temperature(m.max())).withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
        return lines;
    }
    private int hoveredPort(double x,double y) {
        if(!isHovering(60,36,122,148,x,y))return -1;
        int best=-1;
        for(int i=0;i<dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity.MAX_PORTS;i++) if(menu.value(65+i*3)>=0 && Math.abs(x-portPositions[i][0])<7 && Math.abs(y-portPositions[i][1])<7 && (best<0||portPositions[i][2]>portPositions[best][2]))best=i;
        return best;
    }
    @Override public boolean mouseClicked(double x,double y,int b){
        if(b==0){
            if(vaultPanel && isHovering(drawerX(),28,DRAWER_W,DRAWER_H,x,y)){
                int d=drawerX();
                if(isHovering(d+121,38,16,14,x,y)){vaultPanel=false;return true;}
                if(isHovering(d+11,56,26,16,x,y)){nextPort(-1);return true;}
                if(isHovering(d+111,56,26,16,x,y)){nextPort(1);return true;}
                if(selectedFace>=0){
                    for(int m=0;m<3;m++)if(isHovering(d+11,78+m*18,126,16,x,y)){action(dev.zsskayr.merlins_inferno.menu.HellForgeMenu.SET_MODE+selectedFace*3+m);return true;}
                    if(isHovering(d+11,136,126,16,x,y)){action(dev.zsskayr.merlins_inferno.menu.HellForgeMenu.AUTO+selectedFace);return true;}
                    for(int i=0;i<9;i++)if(i<menu.getProcessingSlots()&&isHovering(d+11+i*14,168,13,16,x,y)){action(dev.zsskayr.merlins_inferno.menu.HellForgeMenu.MASK+selectedFace*9+i);return true;}
                }
                return true;
            }
            if(isHovering(1,83,22,22,x,y)){vaultPanel=!vaultPanel;if(selectedFace<0)nextPort(1);return true;}
            int port=hoveredPort(x,y);if(port>=0){selectedFace=port;vaultPanel=true;return true;}
            if(isHovering(1,109,22,22,x,y)){ForgeClientConfig.cycle();return true;}
            if(isHovering(1,57,22,22,x,y)){action(1);return true;}
            if(isHovering(317,273,22,18,x,y)){action(0);return true;}
            if(isHovering(321,38,14,200,x,y)){setFillLimit(y);return true;}
        }
        return super.mouseClicked(x,y,b);
    }
    /** Clicking or dragging on the fuel gauge sets the fuel cap, and so how hot the fire may burn. */
    private void setFillLimit(double mouseY){
        double fraction=Math.clamp((topPos+236-mouseY)/196.0,0,1);
        action(dev.zsskayr.merlins_inferno.menu.HellForgeMenu.FILL_LIMIT+(int)Math.round(fraction*1000));
    }
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy) {
        if(b==0&&isHovering(321,38,14,200,x,y)){setFillLimit(y);return true;}
        if(isHovering(60,36,122,148,x,y)){
            if(b==0){rotation+=(float)dx*2;pitch=Math.clamp(pitch+(float)dy*1.5F,-89F,89F);tipHidden=true;return true;}
            if(b==1){panX=Math.clamp(panX+(float)dx,-70F,70F);panY=Math.clamp(panY+(float)dy,-70F,70F);tipHidden=true;return true;}
        }
        return super.mouseDragged(x,y,b,dx,dy);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(isHovering(60,36,122,148,x,y)){zoom=Math.clamp(zoom+(float)sy*0.1F,0.5F,2.5F);return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void render(GuiGraphics g,int x,int y,float pt) {
        super.render(g,x,y,pt);
        if(vaultPanel){g.pose().pushPose();g.pose().translate(0,0,400);renderDrawer(g);g.pose().popPose();}
        if(!vaultPanel||!isHovering(drawerX(),28,DRAWER_W,DRAWER_H,x,y))renderTooltip(g,x,y);
        if(isHovering(1,57,22,22,x,y))g.renderTooltip(font,Component.literal("Auto Sort: "+(menu.value(12)==1?"ON":"OFF")),x,y);
        if(isHovering(1,83,22,22,x,y))g.renderTooltip(font,Component.literal("Vault Settings"),x,y);
        if(isHovering(1,109,22,22,x,y))g.renderTooltip(font,Component.literal("Temperature Unit"),x,y);
        if(isHovering(321,38,14,200,x,y)){
            List<Component> fuelLines=new ArrayList<>();
            fuelLines.add(Component.literal("Fuel: "+menu.getStoredFuel()+" / "+menu.getFuelCapacity()));
            fuelLines.add(text(menu.magmatic()?"reserve":"fill_limit",Math.round(menu.fillLimit()*100)).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
            if(menu.redstoneLevel()>=0)fuelLines.add(text("fill_redstone",menu.redstoneLevel(),Math.round(menu.effectiveFillLimit()*100)).copy().withStyle(net.minecraft.ChatFormatting.RED));
            g.renderComponentTooltip(font,fuelLines,x,y);
        }
        if(isHovering(40,36,20,168,x,y)&&minecraft.level!=null)g.renderComponentTooltip(font,materialLines(),x,y);
        if(isHovering(317,273,22,18,x,y))g.renderTooltip(font,Component.literal("Dump Fuel"),x,y);
        if(!isHovering(60,36,122,148,x,y))tipHidden=false;
        if(!tipHidden&&isHovering(60,36,122,148,x,y)&&!vaultPanelCovers(x,y))g.renderTooltip(font,text("ports_help"),x,y);
    }
}
