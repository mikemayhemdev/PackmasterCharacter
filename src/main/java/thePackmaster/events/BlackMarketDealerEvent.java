package thePackmaster.events;

import basemod.abstracts.events.PhasedEvent;
import basemod.abstracts.events.phases.TextPhase;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.curses.Necronomicurse;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.city.Beggar;
import com.megacrit.cardcrawl.events.city.TheLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import thePackmaster.SpireAnniversary5Mod;
import thePackmaster.cards.PackRip;
import thePackmaster.hats.Hats;
import thePackmaster.packs.AbstractCardPack;
import thePackmaster.packs.CoreSetPack;
import thePackmaster.potions.PackInAJar;
import thePackmaster.relics.BanishingDecree;
import thePackmaster.relics.PMCollection;

import java.util.ArrayList;
import java.util.Objects;

import static thePackmaster.SpireAnniversary5Mod.*;

public class BlackMarketDealerEvent extends PhasedEvent {
    public static final String ID = makeID("BlackMarketDealerEvent");
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString(ID);
    private static final String[] DESCRIPTIONS = eventStrings.DESCRIPTIONS;
    private static final String[] OPTIONS = eventStrings.OPTIONS;
    private boolean forRemoval = false;
    private int choice = 0;

    public BlackMarketDealerEvent() {
        super(NAME, eventStrings.NAME, SpireAnniversary5Mod.makeImagePath("events/blackMarket.png"));

        registerPhase("base", new TextPhase(DESCRIPTIONS[0])
                .addOption(OPTIONS[0], ((t) -> this.transitionKey("cardDealer")))
                .addOption(OPTIONS[1], (t) -> this.transitionKey("magicDealer"))
                .addOption(OPTIONS[2], (t) -> this.transitionKey("relicDealer"))
                .addOption(OPTIONS[3], (t) -> this.openMap())
        );

        registerPhase("base2", new TextPhase(DESCRIPTIONS[13])
                .addOption(OPTIONS[0], ((t) -> this.transitionKey("cardDealer")))
                .addOption(OPTIONS[1], (t) -> this.transitionKey("magicDealer"))
                .addOption(OPTIONS[2], (t) -> this.transitionKey("relicDealer"))
                .addOption(OPTIONS[3], (t) -> this.openMap())
        );

        registerPhase("cardDealer", new TextPhase(DESCRIPTIONS[1]) {
                    @Override
                    public void update() {
                        if (!AbstractDungeon.isScreenUp && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
                            if (forRemoval) {
                                AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
                                AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(c, (float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2)));
                                AbstractDungeon.player.masterDeck.removeCard(c);
                            } else {
                                AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
                                AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float) Settings.WIDTH * .33F, (float) Settings.HEIGHT / 2.0F));
                                if (choice == 2) {
                                    c = AbstractDungeon.gridSelectScreen.selectedCards.get(1);
                                    AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float) Settings.WIDTH * .66F, (float) Settings.HEIGHT / 2.0F));
                                }
                                if (choice == 0) {
                                    AbstractDungeon.player.loseGold(getGoldCostForBuy());
                                    transitionKey("cardBuyEnd");
                                }
                            }

                            AbstractDungeon.gridSelectScreen.selectedCards.clear();

                            if (choice == 2) {
                                if (forRemoval) {
                                    forRemoval = false;
                                    AbstractDungeon.gridSelectScreen.open(getCardsForLibraryEffect(), 2, TheLibrary.OPTIONS[4], false, false, false, false);
                                } else {
                                    transitionKey("cardTradeEnd");
                                }
                            } else if (choice == 1) {
                                AbstractDungeon.effectList.add(new RainingGoldEffect(getGoldGainForSell()));
                                AbstractDungeon.player.gainGold(getGoldGainForSell());
                                transitionKey("cardSellEnd");
                            }
                        }
                    }
                }.addOption(new TextPhase.OptionInfo(hasEnoughGoldForBuyCard() ? OPTIONS[4] + getGoldCostForBuy() + OPTIONS[5] : OPTIONS[9] + getGoldCostForBuy() + OPTIONS[7]).enabledCondition(this::hasEnoughGoldForBuyCard), (i) -> {   //Buy

                            choice = 0;
                            forRemoval = false;
                            AbstractDungeon.gridSelectScreen.open(getCardsForLibraryEffect(), 1, TheLibrary.OPTIONS[4], false, false, false, false);
                        })
                        .addOption(new TextPhase.OptionInfo(canRemoveCardsForSell() ? OPTIONS[6] + getGoldGainForSell() + OPTIONS[7] : OPTIONS[10]).enabledCondition(this::canRemoveCardsForSell), (i) -> {   //Sell

                            choice = 1;
                            forRemoval = true;

                            AbstractDungeon.gridSelectScreen.open(getCardsForDraftedPurge(), 1, Beggar.OPTIONS[6], false, false, false, true);
                        })
                        .addOption(new TextPhase.OptionInfo(canRemoveCardsForSell() ? OPTIONS[8] : OPTIONS[10]).enabledCondition(this::canRemoveCardsForSell), (i) -> {     //Trade In

                            choice = 2;
                            forRemoval = true;
                            AbstractDungeon.gridSelectScreen.open(getCardsForDraftedPurge(), 1, Beggar.OPTIONS[6], false, false, false, true);

                        })
                        .addOption(OPTIONS[22], ((t) -> this.transitionKey("base2")))
        );

        registerPhase("cardBuyEnd", new TextPhase(DESCRIPTIONS[4]).addOption(OPTIONS[3], (t) -> this.openMap()));
        registerPhase("cardSellEnd", new TextPhase(DESCRIPTIONS[5]).addOption(OPTIONS[3], (t) -> this.openMap()));
        registerPhase("cardTradeEnd", new TextPhase(DESCRIPTIONS[6]).addOption(OPTIONS[3], (t) -> this.openMap()));


        registerPhase("magicDealer", new TextPhase(DESCRIPTIONS[2]) {
                    @Override
                    public void update() {
                        if (!AbstractDungeon.isScreenUp && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
                            AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
                            AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(c, (float) (Settings.WIDTH / 2), (float) (Settings.HEIGHT / 2)));
                            AbstractDungeon.player.masterDeck.removeCard(c);

                            AbstractDungeon.gridSelectScreen.selectedCards.clear();

                            transitionKey("magicCleanseEnd");

                        }
                    }
                }

                        .addOption(new TextPhase.OptionInfo(OPTIONS[11], new PackRip()), (i) -> {   //Curse & Pack Rip
                            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new Necronomicurse(), (Settings.WIDTH * .33F), (float) (Settings.HEIGHT / 2)));
                            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new PackRip(), (Settings.WIDTH * .66F), (float) (Settings.HEIGHT / 2)));
                            transitionKey("magicLearnEnd");
                        }).addOption(OPTIONS[12], (i) -> {   //Pack in a Jar Potion
                            AbstractDungeon.getCurrRoom().rewards.clear();
                            AbstractDungeon.getCurrRoom().rewards.add(new RewardItem(new PackInAJar()));
                            skipDefaultCardRewards = true;
                            AbstractDungeon.combatRewardScreen.open();
                            skipDefaultCardRewards = false;
                            transitionKey("magicSampleEnd");
                        })
                        .addOption(new TextPhase.OptionInfo(canRemoveCardsForCleanse() ? OPTIONS[13] : OPTIONS[14]).enabledCondition(this::canRemoveCardsForCleanse), (i) -> {   //Cleansing Ritual
                            ArrayList<AbstractCard> purgeables = new ArrayList<>();
                            for (AbstractCard c : AbstractDungeon.player.masterDeck.group
                            ) {
                                if (c.type == AbstractCard.CardType.CURSE) {
                                    purgeables.add(c);
                                }
                            }

                            CardGroup purgablesGroup = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
                            purgablesGroup.group = purgeables;
                            AbstractDungeon.gridSelectScreen.open(purgablesGroup, 1, Beggar.OPTIONS[6], false, false, false, true);

                        })
                        .addOption(OPTIONS[22], ((t) -> this.transitionKey("base2")))
        );


        registerPhase("magicLearnEnd", new TextPhase(DESCRIPTIONS[7]).addOption(OPTIONS[3], (t) -> this.openMap()));
        registerPhase("magicSampleEnd", new TextPhase(DESCRIPTIONS[8]).addOption(OPTIONS[3], (t) -> this.openMap()));
        registerPhase("magicCleanseEnd", new TextPhase(DESCRIPTIONS[9]).addOption(OPTIONS[3], (t) -> this.openMap()));


        registerPhase("relicDealer", new TextPhase(DESCRIPTIONS[3])


                .addOption(new TextPhase.OptionInfo(hasEnoughGoldForBuyCollection() ? OPTIONS[15] + getGoldCostForBuyCollection() + OPTIONS[16] : OPTIONS[9] + getGoldCostForBuyCollection() + OPTIONS[7], new PMCollection()).enabledCondition(this::hasEnoughGoldForBuyCollection), (i) -> {   //PM Collection
                    AbstractDungeon.getCurrRoom().spawnRelicAndObtain((float) Settings.WIDTH / 2.0F, (float) Settings.HEIGHT / 2.0F, new PMCollection());

                    transitionKey("relicCollectionEnd");
                })

                .addOption(new TextPhase.OptionInfo(hasEnoughGoldForBuyDecree() ? OPTIONS[17] + getGoldCostForBuyDecree() + OPTIONS[18] : OPTIONS[9] + getGoldCostForBuyDecree() + OPTIONS[7], new BanishingDecree()).enabledCondition(this::hasEnoughGoldForBuyDecree), (i) -> {   //Banishing Decree
                    AbstractDungeon.getCurrRoom().spawnRelicAndObtain((float) Settings.WIDTH / 2.0F, (float) Settings.HEIGHT / 2.0F, new BanishingDecree());
                    transitionKey("relicBanishingEnd");
                })

                .addOption(new TextPhase.OptionInfo(hasHat() ? OPTIONS[19] + getGoldGainForSellHat() + OPTIONS[20] : OPTIONS[21]).enabledCondition(this::hasHat), (i) -> {
                    AbstractDungeon.effectList.add(new RainingGoldEffect(getGoldGainForSellHat()));
                    AbstractDungeon.player.gainGold(getGoldGainForSellHat());
                    Hats.addHat(true, CoreSetPack.ID);
                    Hats.currentHat = CoreSetPack.ID;
                    transitionKey("relicSellHatEnd");
                })

                .addOption(OPTIONS[22], ((t) -> this.transitionKey("base2")))
        );


        registerPhase("relicCollectionEnd", new TextPhase(DESCRIPTIONS[10]).addOption(OPTIONS[3], (t) -> this.openMap()));
        registerPhase("relicBanishingEnd", new TextPhase(DESCRIPTIONS[11]).addOption(OPTIONS[3], (t) -> this.openMap()));
        registerPhase("relicSellHatEnd", new TextPhase(DESCRIPTIONS[12]).addOption(OPTIONS[3], (t) -> this.openMap()));


        transitionKey("base");
    }


    private boolean hasEnoughGoldForBuyCard() {
        return AbstractDungeon.player.gold >= getGoldCostForBuy();
    }

    private boolean hasEnoughGoldForBuyCollection() {
        return AbstractDungeon.player.gold >= getGoldCostForBuyCollection();
    }

    private boolean hasEnoughGoldForBuyDecree() {
        return AbstractDungeon.player.gold >= getGoldCostForBuyDecree();
    }

    private boolean hasHat() {
        return !Objects.equals(Hats.currentHat, CoreSetPack.ID);
    }


    private boolean canRemoveCardsForSell() {
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group
        ) {
            if (c.rarity != AbstractCard.CardRarity.BASIC && c.type != AbstractCard.CardType.CURSE) {
                return true;
            }
        }
        return false;
    }


    private boolean canRemoveCardsForCleanse() {
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group
        ) {
            if (c.type == AbstractCard.CardType.CURSE) {
                return true;
            }
        }
        return false;
    }


    private int getGoldGainForSell() {
        if (AbstractDungeon.ascensionLevel >= 15) {
            return 80;
        } else {
            return 100;
        }
    }

    private int getGoldCostForBuy() {
        if (AbstractDungeon.ascensionLevel >= 15) {
            return 60;
        } else {
            return 50;
        }
    }

    private int getGoldGainForSellHat() {
        if (AbstractDungeon.ascensionLevel >= 15) {
            return 60;
        } else {
            return 75;
        }
    }

    private int getGoldCostForBuyCollection() {
        if (AbstractDungeon.ascensionLevel >= 15) {
            return 120;
        } else {
            return 100;
        }
    }

    private int getGoldCostForBuyDecree() {
        if (AbstractDungeon.ascensionLevel >= 15) {
            return 90;
        } else {
            return 75;
        }
    }

    private CardGroup getCardsForLibraryEffect() {
        //Might be a more effective way to do this?  The main Library uses the standard issue card rewards,
        //but this event can appear in any act, which would skew this option to be too weak in act 1 and too
        //strong in act 3.  It should reflect the usual average result in the actual Library, which is commons
        //and uncommons only.

        ArrayList<AbstractCard> buyables = new ArrayList<>();
        ArrayList<AbstractCardPack> validPacks = new ArrayList<>(allPacks);
        if (!allPacksMode) {
            for (AbstractCardPack p : currentPoolPacks
            ) {
                validPacks.remove(p);
            }
        }
        AbstractCard c;
        AbstractCardPack p;
        while (buyables.size() < 20) {
            p = validPacks.get(AbstractDungeon.cardRandomRng.random(0, validPacks.size() - 1));
            c = p.cards.get(AbstractDungeon.cardRandomRng.random(0, p.cards.size() - 1));
            if (!buyables.contains(c) && c.rarity == AbstractCard.CardRarity.COMMON || c.rarity == AbstractCard.CardRarity.UNCOMMON) {
                buyables.add(c.makeCopy());
            }
        }
        CardGroup buyablesGroup = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        buyablesGroup.group = buyables;
        return buyablesGroup;
    }

    private CardGroup getCardsForDraftedPurge() {
        ArrayList<AbstractCard> purgeables = new ArrayList<>();
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group
        ) {
            if (c.rarity != AbstractCard.CardRarity.BASIC && c.type != AbstractCard.CardType.CURSE) {
                purgeables.add(c);
            }
        }

        CardGroup purgablesGroup = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        purgablesGroup.group = purgeables;
        return purgablesGroup;
    }

}
