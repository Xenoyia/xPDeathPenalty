package com.xpgaming.xPDeathPenalty;
import net.minecraft.util.DamageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Optional;

@Plugin(id = Main.id, name = Main.name, version = "0.2")
public class Main {
    public static final String id = "xpdeathpenalty";
    public static final String name = "xP// Death Penalty";
    private static final Logger log = LoggerFactory.getLogger(name);

    private static EconomyService economyService;

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getService().equals(EconomyService.class)) {
            economyService = (EconomyService) event.getNewProviderRegistration().getProvider();
        }
    }

    @Listener (beforeModifications = true)
    public void onPlayerDeath(DestructEntityEvent.Death event, @First DamageSource damageSrc)
    {
        if (event.getTargetEntity() instanceof Player) {
            boolean glitchedDeath = false;
            String reasonForDeath = "other";
            if(damageSrc == DamageSource.inWall) {
                reasonForDeath = "suffocated in wall";
                glitchedDeath = true;
            }
            else if(damageSrc == DamageSource.outOfWorld) {
                reasonForDeath = "fell out of world";
                glitchedDeath = true;
            }
            else if(damageSrc == DamageSource.flyIntoWall) {
                reasonForDeath = "flew into wall";
                glitchedDeath = true;
            } else if(damageSrc == DamageSource.fall) {
                reasonForDeath = "fall damage";
            } else if(damageSrc == DamageSource.fallingBlock) {
                reasonForDeath = "falling blocks";
            }
            Player player = (Player) event.getTargetEntity();
            Optional<UniqueAccount> uOpt = economyService.getOrCreateAccount(player.getUniqueId());
            if (uOpt.isPresent()) {
                UniqueAccount account = uOpt.get();
                BigDecimal econBalance = account.getBalance(economyService.getDefaultCurrency());
                BigDecimal amountToWithdraw = econBalance.multiply(BigDecimal.valueOf(0.07));
                DecimalFormat df = new DecimalFormat("#.##");
                if(amountToWithdraw.doubleValue() > 500) {
                    //maximum of 500 coins taken.
                    amountToWithdraw = BigDecimal.valueOf(500);
                }
                if(!player.getWorld().getName().equalsIgnoreCase("Event") && !player.getWorld().getName().equalsIgnoreCase("Hub") && !player.getWorld().getName().equalsIgnoreCase("Creative")) {
                    if (glitchedDeath) {
                        player.sendMessage(Text.of("\u00A7f[\u00A76Death Penalty\u00A7f] \u00A76You died in a strange way, no money was taken!"));
                        log.info(player.getName() + " had a glitched death at X" + df.format(player.getLocation().getX()) + " Y" + df.format(player.getLocation().getY()) + " Z" + df.format(player.getLocation().getZ()) + ". They would've lost " + amountToWithdraw + " coins.");
                    } else {
                        removeMoney(player, amountToWithdraw);
                        player.sendMessage(Text.of("\u00A7f[\u00A76Death Penalty\u00A7f] \u00A76You lost \u00A7e" + amountToWithdraw.setScale(2, RoundingMode.CEILING) + " coins \u00A76from dying!"));
                        log.info(player.getName() + " had a non-glitched death at X" + df.format(player.getLocation().getX()) + " Y" + df.format(player.getLocation().getY()) + " Z" + df.format(player.getLocation().getZ()) + ". They lost " + amountToWithdraw + " coins.");
                    }
                }
            }
        }
    }

    public void removeMoney(Player p, BigDecimal amount) {
        Optional<UniqueAccount> uOpt = economyService.getOrCreateAccount(p.getUniqueId());
        if(uOpt.isPresent()) {
            UniqueAccount account = uOpt.get();
            TransactionResult result = account.withdraw(economyService.getDefaultCurrency(), amount, Cause.source(this).build());
            if (!(result.getResult() == ResultType.SUCCESS)) {
                p.sendMessage(Text.of("\u00A7f[\u00A7cDeath Penalty\u00A7f] \u00A7cUnable to give money, something broke!"));
            }
        }
    }

    @Listener (beforeModifications = true)
    public void onGameInitialization(GameInitializationEvent event) {
        log.info("Loaded v0.2!");
    }
}
