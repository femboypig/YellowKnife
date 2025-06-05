package net.femboypig.yellowknife.mixin;

import net.femboypig.yellowknife.screen.AccountScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    
    protected TitleScreenMixin(Text title) {
        super(title);
    }
    
    @Inject(method = "init()V", at = @At("RETURN"))
    private void addAccountButton(CallbackInfo ci) {
        // Find all buttons on the screen
        List<ButtonWidget> buttons = this.children()
                .stream()
                .filter(child -> child instanceof ButtonWidget)
                .map(child -> (ButtonWidget) child)
                .collect(Collectors.toList());

        // Find the Multiplayer button (usually contains text "Multiplayer")
        ButtonWidget multiplayerButton = null;
        for (ButtonWidget button : buttons) {
            String buttonText = button.getMessage().getString().toLowerCase();
            if (buttonText.contains("multiplayer") || buttonText.contains("сетевая игра")) {
                multiplayerButton = button;
                break;
            }
        }

        // If the Multiplayer button is found, place the Account button next to it
        if (multiplayerButton != null) {
            int x = multiplayerButton.getX() + multiplayerButton.getWidth() + 4;
            int y = multiplayerButton.getY();
            int width = 98; // About half the width of a standard button
            int height = multiplayerButton.getHeight();

            this.addDrawableChild(
                ButtonWidget.builder(
                    Text.translatable("yellowknife.account.button"),
                    button -> this.client.setScreen(new AccountScreen((TitleScreen)(Object)this))
                ).dimensions(x, y, width, height).build()
            );
        } else {
            // Fallback if the button is not found - use standard coordinates
            this.addDrawableChild(
                ButtonWidget.builder(
                    Text.translatable("yellowknife.account.button"),
                    button -> this.client.setScreen(new AccountScreen((TitleScreen)(Object)this))
                ).dimensions(this.width / 2 + 4, 72, 98, 20).build()
            );
        }
    }
    
    @Inject(at = @At("TAIL"), method = "render")
    private void renderCurrentAccount(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        String currentUsername = MinecraftClient.getInstance().getSession().getUsername();
        
        Text currentAccountText = Text.translatable("yellowknife.account.current_account").formatted(Formatting.WHITE)
                                      .append(Text.literal(currentUsername).formatted(Formatting.GREEN))
                                      .append(Text.literal("").formatted(Formatting.WHITE));
        
        context.drawCenteredTextWithShadow(
            this.textRenderer, 
            currentAccountText, 
            this.width / 2, 
            this.height - 10, 
            0xFFFFFF
        );
    }
} 