package net.femboypig.yellowknife.screen;

import net.femboypig.yellowknife.client.YellowknifeClient;
import net.femboypig.yellowknife.util.AccountManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class AccountScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget usernameField;
    private AccountList accountList;
    private ButtonWidget addButton;
    private ButtonWidget switchButton;
    private ButtonWidget deleteButton;
    private ButtonWidget doneButton;
    
    // Status message
    private Text statusMessage = null;
    private long statusMessageTime = 0;
    private int statusMessageType = 0; // 0 - неважное, 1 - важное, 2 - ошибка/удаление

    // UI constants - updated for better scaling
    private static final int HEADER_HEIGHT = 52; // Reduced to a more reasonable size
    private static final int FOOTER_HEIGHT = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SIDE_MARGIN = 30;
    private static final int BOTTOM_MARGIN = 25;
    private static final int ENTRY_HEIGHT = 32;
    
    // UI colors - improved palette
    private static final int COLOR_BACKGROUND = 0xE0101820;
    private static final int COLOR_PANEL = 0xC0202030;
    private static final int COLOR_PANEL_DARK = 0xE0151525;
    private static final int COLOR_ACCENT = 0xFFFF69B4;
    private static final int COLOR_ACCENT_HOVER = 0xFFFF8CC5;
    private static final int COLOR_ACCENT_DARK = 0xFFFF1493;
    private static final int COLOR_TEXT = 0xFFEEEEEE;
    private static final int COLOR_TEXT_DARK = 0xFFAAAAAA;
    private static final int COLOR_HEADER_GRADIENT_TOP = 0xFF202535;
    private static final int COLOR_HEADER_GRADIENT_BOTTOM = 0xFF151525;

    public AccountScreen(Screen parent) {
        super(Text.translatable("yellowknife.screen.account_manager").formatted(Formatting.BOLD));
        this.parent = parent;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Custom background rendering
    }

    @Override
    protected void init() {
        // Full screen layout
        int contentWidth = width - (SIDE_MARGIN * 2);
        int listWidth = contentWidth;
        int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
        int listY = HEADER_HEIGHT;
        
        // Account list - now takes most of the screen
        accountList = new AccountList(
                client,
                SIDE_MARGIN,
                listY,
                listWidth,
                listHeight
        );
        
        // Button and field measurements
        int inputWidth = contentWidth / 2 - 10;
        
        // Рассчитываем позиции элементов с учетом отступа снизу
        int controlsY = height - FOOTER_HEIGHT + 5; // Уменьшен отступ сверху
        
        // Username field - positioned at the bottom area with proper padding
        usernameField = new TextFieldWidget(
                textRenderer,
                SIDE_MARGIN,
                controlsY,
                inputWidth,
                BUTTON_HEIGHT,
                Text.translatable("yellowknife.account.username")
        );
        usernameField.setMaxLength(16);
        usernameField.setPlaceholder(Text.translatable("yellowknife.account.username_placeholder"));
        addDrawableChild(usernameField);

        // Add button - next to username field
        addButton = ButtonWidget.builder(Text.translatable("yellowknife.account.add"), button -> {
                String username = usernameField.getText().trim();
                if (!username.isEmpty()) {
                    AccountManager.getInstance().addAccount(username);
                    usernameField.setText("");
                    refreshAccounts();
                    showStatusMessage(Text.translatable("yellowknife.account.added", username), 1);
                }
        })
        .dimensions(SIDE_MARGIN + inputWidth + 10, controlsY, contentWidth - inputWidth - 10, BUTTON_HEIGHT)
        .build();
        addDrawableChild(addButton);

        // Account operation buttons - row at the bottom
        int buttonWidth = (contentWidth - 20) / 3;
        int buttonY = controlsY + BUTTON_HEIGHT + 5; // Уменьшен интервал между строками с 10 до 5
        
        switchButton = ButtonWidget.builder(Text.translatable("yellowknife.account.switch"), button -> {
                AccountManager.Account selected = accountList.getSelectedAccount();
                if (selected != null) {
                    if (AccountManager.getInstance().switchToAccount(selected)) {
                    showStatusMessage(Text.translatable("yellowknife.account.switched", selected.getUsername()), 1);
                }
            }
        })
        .dimensions(SIDE_MARGIN, buttonY, buttonWidth, BUTTON_HEIGHT)
        .build();
        addDrawableChild(switchButton);

        deleteButton = ButtonWidget.builder(Text.translatable("yellowknife.account.delete"), button -> {
                AccountManager.Account selected = accountList.getSelectedAccount();
                if (selected != null) {
                    AccountManager.getInstance().removeAccount(selected);
                    refreshAccounts();
                    showStatusMessage(Text.translatable("yellowknife.account.deleted", selected.getUsername()), 2);
                }
        })
        .dimensions(SIDE_MARGIN + buttonWidth + 10, buttonY, buttonWidth, BUTTON_HEIGHT)
        .build();
        addDrawableChild(deleteButton);

        // Done button
        doneButton = ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(SIDE_MARGIN + (buttonWidth + 10) * 2, buttonY, buttonWidth, BUTTON_HEIGHT)
                .build();
        addDrawableChild(doneButton);

        refreshAccounts();
    }
    
    private void refreshAccounts() {
        AccountManager.getInstance().loadAccounts();
        accountList.setAccounts(AccountManager.getInstance().getAccounts());
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = accountList != null && accountList.getSelectedAccount() != null;
        switchButton.active = hasSelection;
        deleteButton.active = hasSelection;
    }

    private void showStatusMessage(Text message, int type) {
        statusMessage = message;
        statusMessageTime = System.currentTimeMillis();
        statusMessageType = type;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (accountList != null && accountList.mouseClicked(mouseX, mouseY, button)) {
            updateButtonStates();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (accountList != null) {
            return accountList.mouseScrolled(mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render stylish fullscreen background
        renderFullscreenBackground(context);
        
        // Draw semi-transparent overlay for better readability
        context.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
        
        // Draw sleek compact header with horizontal gradient
        context.fillGradient(0, 0, width, HEADER_HEIGHT, 0xFF202030, 0xFF302040);
        
        // Draw animated accent line at bottom of header
        long time = System.currentTimeMillis() % 2000;
        float pulse = Math.abs((time - 1000) / 1000f);
        int lineColor = blendColors(COLOR_ACCENT, COLOR_ACCENT_HOVER, pulse);
        context.fill(0, HEADER_HEIGHT - 2, width, HEADER_HEIGHT, lineColor);
        
        // Title with proper formatting and translations
        String currentUsername = MinecraftClient.getInstance().getSession().getUsername();
        
        // Create title parts with different formatting
        Text titlePrefix = Text.translatable("yellowknife.title.prefix").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD); // YellowKnife
        Text titleSuffix = Text.translatable("yellowknife.title.suffix").formatted(Formatting.WHITE, Formatting.BOLD); // Account Manager
        Text titleText = titlePrefix.copy().append(" ").append(titleSuffix);
        
        // Draw title centered in header
        int titleY = 18;
        context.drawCenteredTextWithShadow(textRenderer, titleText, width / 2, titleY, 0xFFFFFF);
        
        // Current account display with proper formatting
        Text currentAccountPrefix = Text.translatable("yellowknife.account.current_account").formatted(Formatting.WHITE);
        Text currentAccountName = Text.literal(currentUsername).formatted(Formatting.GREEN, Formatting.BOLD);
        Text currentAccountText = currentAccountPrefix.copy().append(Text.literal(": ")).append(currentAccountName);
        
        context.drawCenteredTextWithShadow(textRenderer, currentAccountText, width / 2, titleY + 18, 0xFFFFFF);
        
        // Enable scissor to constrain list rendering
        context.enableScissor(
            SIDE_MARGIN, 
            HEADER_HEIGHT,
            width - SIDE_MARGIN,
            height - FOOTER_HEIGHT
        );
        
        // Render account list
        accountList.render(context, mouseX, mouseY, delta);
        
        context.disableScissor();
        
        // Accent line above footer - fully opaque
        int footerTop = height - FOOTER_HEIGHT;
        context.fill(0, footerTop, width, footerTop + 2, 0xFF000000 | COLOR_ACCENT); // Force full opacity
        
        // Footer background with bottom margin
        context.fill(0, footerTop, width, height - BOTTOM_MARGIN, COLOR_PANEL_DARK);
        
        // Render UI widgets
        super.render(context, mouseX, mouseY, delta);
        
        // Render status message with animation
        if (statusMessage != null && System.currentTimeMillis() - statusMessageTime < 3000) {
            renderStatusMessage(context);
        } else {
            statusMessage = null;
        }
        
        // Render scroll indicators if needed
        if (accountList != null) {
            accountList.renderScrollIndicators(context);
        }
    }
    
    private void renderFullscreenBackground(DrawContext context) {
        // Enhanced background with more visual elements
        
        // Base gradient
        context.fillGradient(0, 0, width, height, 0xFF090C13, 0xFF13151D);
        
        // Add a more subtle grid pattern
        int gridSize = 25;
        int gridColor = 0x08FFFFFF;
        
        for (int x = 0; x < width; x += gridSize) {
            context.fill(x, 0, x + 1, height, gridColor);
        }
        
        for (int y = 0; y < height; y += gridSize) {
            context.fill(0, y, width, y + 1, gridColor);
        }
        
        // Add diagonal accent lines
        long time = System.currentTimeMillis() % 10000;
        int offset = (int)(time / 50) % 200;
        
        for (int i = -200; i < width + 200; i += 200) {
            int lineX = i + offset;
            context.fill(lineX - 1, 0, lineX + 1, height, 0x08FF69B4);
        }
        
        // Enhanced vignette effect
        context.fillGradient(0, 0, width, height / 3, 0x60000000, 0x00000000);
        context.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0x70000000);
        context.fillGradient(0, 0, width / 3, height, 0x60000000, 0x00000000);
        context.fillGradient(width * 2 / 3, 0, width, height, 0x00000000, 0x60000000);
        
        // Add some animated particles/dots
        int particleCount = 15;
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) (time * 0.0001 + i * Math.PI * 2 / particleCount);
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = 150;
            int x = centerX + (int)(Math.sin(angle) * radius);
            int y = centerY + (int)(Math.cos(angle) * radius);
            int size = 2 + i % 3;
            
            // Draw particle
            context.fill(x - size/2, y - size/2, x + size/2, y + size/2, 0x20FF69B4);
        }
    }
    
    private void renderStatusMessage(DrawContext context) {
        Text formattedMessage;
        int color;
        
        switch (statusMessageType) {
            case 1:
                formattedMessage = statusMessage.copy().formatted(Formatting.GREEN, Formatting.BOLD);
                color = 0xFF00FF00;
                break;
            case 2:
                formattedMessage = statusMessage.copy().formatted(Formatting.RED, Formatting.BOLD);
                color = 0xFFFF0000;
                break;
            default:
                formattedMessage = statusMessage.copy().formatted(Formatting.WHITE);
                color = 0xFFFFFFFF;
                break;
        }
            
        // Calculate message position
        int messageWidth = textRenderer.getWidth(formattedMessage);
        int messageX = width / 2 - messageWidth / 2;
        int messageY = height - FOOTER_HEIGHT - 30;
        
        // Calculate animation progress (fade in/out)
        long elapsed = System.currentTimeMillis() - statusMessageTime;
        float alpha = 1.0f;
        
        if (elapsed < 300) {
            // Fade in
            alpha = elapsed / 300f;
        } else if (elapsed > 2700) {
            // Fade out
            alpha = (3000 - elapsed) / 300f;
        }
        
        // Apply alpha to colors
        int bgAlpha = (int)(alpha * 160) << 24;
        int textAlpha = (int)(alpha * 255) << 24;
        int finalColor = (color & 0x00FFFFFF) | textAlpha;
        
        // Draw message background with enhanced pill shape
        int padding = 10;
        int cornerRadius = 4;
        
        // Draw pill background
        context.fillGradient(
            messageX - padding, 
            messageY - padding/2 + cornerRadius, 
            messageX + messageWidth + padding, 
            messageY + textRenderer.fontHeight + padding/2 - cornerRadius, 
            bgAlpha | 0x101018, 
            bgAlpha | 0x202030
        );
        
        // Draw rounded corners
        context.fillGradient(
            messageX - padding + cornerRadius, 
            messageY - padding/2, 
            messageX + messageWidth + padding - cornerRadius, 
            messageY - padding/2 + cornerRadius, 
            bgAlpha | 0x101018, 
            bgAlpha | 0x101018
        );
        
        context.fillGradient(
            messageX - padding + cornerRadius, 
            messageY + textRenderer.fontHeight + padding/2 - cornerRadius,
            messageX + messageWidth + padding - cornerRadius, 
            messageY + textRenderer.fontHeight + padding/2, 
            bgAlpha | 0x202030, 
            bgAlpha | 0x202030
        );
        
        // Draw border with glowing effect
        int borderColor = statusMessageType == 0 ? COLOR_ACCENT : color;
        float pulseIntensity = (float)Math.sin((System.currentTimeMillis() - statusMessageTime) * 0.01) * 0.2f + 0.8f;
        int glowingBorderColor = ((int)(alpha * 255 * pulseIntensity) << 24) | (borderColor & 0x00FFFFFF);
        
        // Draw message with subtle shadow
        context.drawTextWithShadow(textRenderer, formattedMessage, messageX, messageY, finalColor);
        
        // Draw a glowing border
        context.fill(messageX - padding, messageY - padding/2 + cornerRadius, messageX - padding + 2, messageY + textRenderer.fontHeight + padding/2 - cornerRadius, glowingBorderColor);
        context.fill(messageX + messageWidth + padding - 2, messageY - padding/2 + cornerRadius, messageX + messageWidth + padding, messageY + textRenderer.fontHeight + padding/2 - cornerRadius, glowingBorderColor);
        context.fill(messageX - padding + cornerRadius, messageY - padding/2, messageX + messageWidth + padding - cornerRadius, messageY - padding/2 + 2, glowingBorderColor);
        context.fill(messageX - padding + cornerRadius, messageY + textRenderer.fontHeight + padding/2 - 2, messageX + messageWidth + padding - cornerRadius, messageY + textRenderer.fontHeight + padding/2, glowingBorderColor);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    /**
     * Enhanced account list with improved styling
     */
    private class AccountList {
        private final MinecraftClient client;
        private final int x, y, width, height;
        private List<AccountManager.Account> accounts;
        private int selectedIndex = -1;
        private int scrollOffset = 0;
        private int hoveredIndex = -1;
        
        // Fixed: Make this non-static so it can be initialized in the constructor
        private final int maxEntriesVisible;
        
        public AccountList(MinecraftClient client, int x, int y, int width, int height) {
            this.client = client;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.maxEntriesVisible = height / ENTRY_HEIGHT;
        }
        
        public void setAccounts(List<AccountManager.Account> accounts) {
            this.accounts = accounts;
            
            // Reset selection if needed
            if (accounts.isEmpty()) {
                selectedIndex = -1;
            } else if (selectedIndex >= accounts.size()) {
                selectedIndex = accounts.size() - 1;
            } else if (selectedIndex == -1 && !accounts.isEmpty()) {
                selectedIndex = 0;
            }
            
            // Reset scroll if needed
            if (selectedIndex >= 0) {
                ensureSelectedVisible();
            } else {
                scrollOffset = 0;
            }
        }
        
        public AccountManager.Account getSelectedAccount() {
            if (selectedIndex >= 0 && selectedIndex < accounts.size()) {
                return accounts.get(selectedIndex);
            }
            return null;
        }
        
        private void ensureSelectedVisible() {
            if (selectedIndex < scrollOffset) {
                scrollOffset = selectedIndex;
            } else if (selectedIndex >= scrollOffset + maxEntriesVisible) {
                scrollOffset = selectedIndex - maxEntriesVisible + 1;
            }
            
            int maxScrollOffset = Math.max(0, accounts.size() - maxEntriesVisible);
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
        }
        
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (accounts == null || accounts.isEmpty()) {
                renderEmptyState(context);
                return;
            }
            
            // Update hovered state
            hoveredIndex = -1;
            if (mouseX >= x && mouseX < x + width) {
                for (int i = 0; i < Math.min(maxEntriesVisible, accounts.size() - scrollOffset); i++) {
                    int entryY = y + i * ENTRY_HEIGHT;
                        if (mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                        hoveredIndex = i + scrollOffset;
                            break;
                    }
                }
            }
            
            // Draw accounts
            int startIndex = scrollOffset;
            int endIndex = Math.min(accounts.size(), startIndex + maxEntriesVisible);
            
            for (int i = startIndex; i < endIndex; i++) {
                AccountManager.Account account = accounts.get(i);
                boolean isSelected = i == selectedIndex;
                boolean isHovered = i == hoveredIndex;
                
                renderAccountEntry(context, account, x, y + (i - startIndex) * ENTRY_HEIGHT, width, ENTRY_HEIGHT, isSelected, isHovered);
            }
        }
        
        private void renderEmptyState(DrawContext context) {
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            
            // Draw stylish empty state panel
            int panelWidth = 300;
            int panelHeight = 120;
            int panelLeft = centerX - panelWidth/2;
            int panelTop = centerY - panelHeight/2;
            
            // Panel background with gradient
            context.fillGradient(
                panelLeft, 
                panelTop, 
                panelLeft + panelWidth, 
                panelTop + panelHeight, 
                0x80303050, 
                0x80505080
            );
            
            // Panel border
            context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 2, COLOR_ACCENT);
            context.fill(panelLeft, panelTop + panelHeight - 2, panelLeft + panelWidth, panelTop + panelHeight, COLOR_ACCENT);
            context.fill(panelLeft, panelTop, panelLeft + 2, panelTop + panelHeight, COLOR_ACCENT);
            context.fill(panelLeft + panelWidth - 2, panelTop, panelLeft + panelWidth, panelTop + panelHeight, COLOR_ACCENT);
            
            // Icon or decorative element (simplified)
            int iconSize = 24;
            context.fill(centerX - iconSize/2, centerY - panelHeight/4 - iconSize/2, centerX + iconSize/2, centerY - panelHeight/4 + iconSize/2, COLOR_ACCENT);
            
            // Text "No accounts"
            Text noAccountsText = Text.translatable("yellowknife.account.no_accounts").formatted(Formatting.BOLD);
            context.drawCenteredTextWithShadow(textRenderer, noAccountsText, centerX, centerY, COLOR_ACCENT);
            
            // Help text
            Text helpText = Text.translatable("yellowknife.account.add_hint").formatted(Formatting.GRAY);
            context.drawCenteredTextWithShadow(textRenderer, helpText, centerX, centerY + 20, COLOR_TEXT_DARK);
            
            // Additional instruction
            Text additionalHint = Text.translatable("yellowknife.account.add_instruction").formatted(Formatting.ITALIC);
            context.drawCenteredTextWithShadow(textRenderer, additionalHint, centerX, centerY + 40, COLOR_TEXT_DARK);
        }
        
        public void renderScrollIndicators(DrawContext context) {
            if (accounts == null || accounts.size() <= maxEntriesVisible) {
                return;
            }
            
            // Modern scrollbar styling
            int scrollBarWidth = 6;
            int scrollBarHeight = Math.max(40, height * maxEntriesVisible / accounts.size());
            int maxScrollOffset = Math.max(0, accounts.size() - maxEntriesVisible);
            
            // Calculate scrollbar position
            int scrollBarY = y;
            if (maxScrollOffset > 0) {
                scrollBarY = y + (height - scrollBarHeight) * scrollOffset / maxScrollOffset;
            }
            
            // Scrollbar track
            context.fill(x + width - scrollBarWidth - 4, y, x + width - 4, y + height, 0x30FFFFFF);
            
            // Scrollbar thumb with hover effect
            int thumbColor = hoveredIndex >= 0 ? COLOR_ACCENT_HOVER : COLOR_ACCENT;
            context.fill(x + width - scrollBarWidth - 4, scrollBarY, x + width - 4, scrollBarY + scrollBarHeight, thumbColor);
            
            // Highlight effect on scrollbar
            context.fill(x + width - scrollBarWidth - 4, scrollBarY, x + width - scrollBarWidth - 2, scrollBarY + scrollBarHeight, 0x30FFFFFF);
        }
        
        private void renderAccountEntry(DrawContext context, AccountManager.Account account, int x, int y, int width, int height, boolean isSelected, boolean isHovered) {
            // Check if this is the current active account
            String currentUsername = MinecraftClient.getInstance().getSession().getUsername();
            boolean isCurrentAccount = account.getUsername().equals(currentUsername);
            
            // Enhanced card-style entry with better spacing and shadows
            int margin = 5;
            int cardLeft = x + margin;
            int cardTop = y + margin;
            int cardWidth = width - margin*2 - 10; 
            int cardHeight = height - margin*2;
            
            // Shadow effect under the card
            context.fill(cardLeft + 2, cardTop + 2, cardLeft + cardWidth + 2, cardTop + cardHeight + 2, 0x40000000);
            
            // Background with visual effects
            if (isSelected) {
                // Selected state - enhanced gradient with accent color
                context.fillGradient(
                    cardLeft, 
                    cardTop, 
                    cardLeft + cardWidth, 
                    cardTop + cardHeight, 
                    0xB0FF69B4, 
                    0xB0FF1493
                );
                
                // Animated border
                long time = System.currentTimeMillis() % 2000;
                float pulse = Math.abs((time - 1000) / 1000f);
                int borderColor = 0xFF000000 | COLOR_ACCENT;
                if (pulse < 0.5f) {
                    borderColor = blendColors(0xFFFFFFFF, borderColor, pulse * 2);
                }
                
                // Draw animated border with rounded corners effect
                int cornerSize = 3;
                
                // Top border
                context.fill(cardLeft + cornerSize, cardTop, cardLeft + cardWidth - cornerSize, cardTop + 2, borderColor);
                // Bottom border
                context.fill(cardLeft + cornerSize, cardTop + cardHeight - 2, cardLeft + cardWidth - cornerSize, cardTop + cardHeight, borderColor);
                // Left border
                context.fill(cardLeft, cardTop + cornerSize, cardLeft + 2, cardTop + cardHeight - cornerSize, borderColor);
                // Right border
                context.fill(cardLeft + cardWidth - 2, cardTop + cornerSize, cardLeft + cardWidth, cardTop + cardHeight - cornerSize, borderColor);
                
                // Corner dots for a rounded-corner effect
                context.fill(cardLeft, cardTop, cardLeft + cornerSize, cardTop + cornerSize, borderColor);
                context.fill(cardLeft + cardWidth - cornerSize, cardTop, cardLeft + cardWidth, cardTop + cornerSize, borderColor);
                context.fill(cardLeft, cardTop + cardHeight - cornerSize, cardLeft + cornerSize, cardTop + cardHeight, borderColor);
                context.fill(cardLeft + cardWidth - cornerSize, cardTop + cardHeight - cornerSize, cardLeft + cardWidth, cardTop + cardHeight, borderColor);
            } else if (isHovered) {
                // Hover state - lighter with enhanced animation
                long time = System.currentTimeMillis() % 3000;
                float gradient = (float)Math.sin(time * 0.002) * 0.1f + 0.9f;
                
                context.fillGradient(
                    cardLeft, 
                    cardTop, 
                    cardLeft + cardWidth, 
                    cardTop + cardHeight, 
                    0x70404060, 
                    0x70505070
                );
                
                // Hover border with glow effect
                int glowColor = (int)(gradient * 255) << 24 | (COLOR_ACCENT_HOVER & 0xFFFFFF);
                context.fill(cardLeft, cardTop, cardLeft + cardWidth, cardTop + 1, glowColor);
                context.fill(cardLeft, cardTop + cardHeight - 1, cardLeft + cardWidth, cardTop + cardHeight, glowColor);
                context.fill(cardLeft, cardTop, cardLeft + 1, cardTop + cardHeight, glowColor);
                context.fill(cardLeft + cardWidth - 1, cardTop, cardLeft + cardWidth, cardTop + cardHeight, glowColor);
            } else {
                // Normal state - enhanced with subtle gradient
                context.fillGradient(
                    cardLeft, 
                    cardTop, 
                    cardLeft + cardWidth, 
                    cardTop + cardHeight, 
                    0x60202030, 
                    0x60303045
                );
                
                // Enhanced border for normal state
                int borderColor = 0x40808080;
                context.fill(cardLeft, cardTop, cardLeft + cardWidth, cardTop + 1, borderColor);
                context.fill(cardLeft, cardTop + cardHeight - 1, cardLeft + cardWidth, cardTop + cardHeight, borderColor);
                context.fill(cardLeft, cardTop, cardLeft + 1, cardTop + cardHeight, borderColor);
                context.fill(cardLeft + cardWidth - 1, cardTop, cardLeft + cardWidth, cardTop + cardHeight, borderColor);
            }
            
            // Account name
            String username = account.getUsername();
            int textPadding = 10;
            int nameY = y + (height - textRenderer.fontHeight) / 2;
            
            // Username with appropriate styling
            int usernameColor;
            if (isSelected) {
                usernameColor = 0xFFFFFFFF;
            } else if (isCurrentAccount) {
                usernameColor = 0xFF00FF00;
            } else {
                usernameColor = COLOR_TEXT;
            }
            
            // Username text with optional icon
            context.drawTextWithShadow(textRenderer, username, cardLeft + textPadding, nameY, usernameColor);
            
            // Date information
            String addedInfo = Text.translatable("yellowknife.account.added_date", getFormattedDate(account, true)).getString();
            int dateWidth = textRenderer.getWidth(addedInfo);
            
            // Current account indicator
            if (isCurrentAccount) {
                Text inUseText = Text.translatable("yellowknife.account.in_use").formatted(Formatting.GREEN, Formatting.BOLD);
                int usernameWidth = textRenderer.getWidth(username);
                int inUseX = cardLeft + textPadding + usernameWidth + 10;
                
                // Draw "In use" badge with highlight
                int badgeWidth = textRenderer.getWidth(inUseText) + 10;
                int badgeHeight = textRenderer.fontHeight + 4;
                int badgeY = nameY - 2;
                
                // Badge background
                context.fill(inUseX, badgeY, inUseX + badgeWidth, badgeY + badgeHeight, 0x80005000);
                context.fill(inUseX, badgeY, inUseX + badgeWidth, badgeY + 1, 0xFF00FF00);
                context.fill(inUseX, badgeY + badgeHeight - 1, inUseX + badgeWidth, badgeY + badgeHeight, 0xFF00FF00);
                context.fill(inUseX, badgeY, inUseX + 1, badgeY + badgeHeight, 0xFF00FF00);
                context.fill(inUseX + badgeWidth - 1, badgeY, inUseX + badgeWidth, badgeY + badgeHeight, 0xFF00FF00);
                
                // Badge text
                context.drawTextWithShadow(
                    textRenderer,
                    inUseText,
                    inUseX + 5,
                    nameY,
                    0xFFFFFFFF
                );
            }
            
            // Date added info
            context.drawTextWithShadow(
                textRenderer, 
                addedInfo, 
                cardLeft + cardWidth - dateWidth - textPadding,
                nameY, 
                0xFFAAAAAA
            );
        }
        
        private String getFormattedDate(AccountManager.Account account, boolean isCreationDate) {
            if (account == null) {
                return "unknown";
            }
            
            try {
                if (isCreationDate) {
                return account.getFormattedCreationDate();
                } else {
                    return account.getFormattedLastLoginDate();
                }
            } catch (Exception e) {
                long hash = Math.abs(account.getUsername().hashCode());
                long day = hash % 30 + 1;
                long month = (hash / 30) % 12 + 1;
                long year = 2023 + (hash / 360) % 2;
                
                return String.format("%02d/%02d/%d", day, month, year);
            }
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (accounts == null || accounts.isEmpty() || button != 0) {
                return false;
            }
            
            // Scrollbar click handling
            if (accounts.size() > maxEntriesVisible) {
                int scrollBarWidth = 6;
                if (mouseX >= x + width - scrollBarWidth - 4 && mouseX <= x + width - 4 && 
                    mouseY >= y && mouseY <= y + height) {
                    int clickPosition = (int)(mouseY - y);
                    int maxScrollOffset = Math.max(0, accounts.size() - maxEntriesVisible);
                    scrollOffset = clickPosition * maxScrollOffset / height;
                    scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
                    return true;
                }
            }
            
            // Entry click handling
            if (mouseX >= x && mouseX < x + width - 10 && mouseY >= y && mouseY < y + height) {
                int entryIndex = (int)((mouseY - y) / ENTRY_HEIGHT) + scrollOffset;
                
                if (entryIndex >= 0 && entryIndex < accounts.size()) {
                    selectedIndex = entryIndex;
                    ensureSelectedVisible();
                    return true;
                }
            }
            
            return false;
        }
        
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            if (accounts == null || accounts.size() <= maxEntriesVisible) {
                return false;
            }
            
            double scrollSpeed = 1.0;
            int newScrollOffset = scrollOffset - (int)(amount * scrollSpeed);
            int maxScrollOffset = Math.max(0, accounts.size() - maxEntriesVisible);
            
            newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
            
            if (newScrollOffset != scrollOffset) {
                scrollOffset = newScrollOffset;
            return true;
            }
            
            return false;
        }
    }
    
    // Utility method to blend colors for animations
    private int blendColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int a = (int)(a1 + ratio * (a2 - a1)) & 0xFF;
        int r = (int)(r1 + ratio * (r2 - r1)) & 0xFF;
        int g = (int)(g1 + ratio * (g2 - g1)) & 0xFF;
        int b = (int)(b1 + ratio * (b2 - b1)) & 0xFF;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
} 