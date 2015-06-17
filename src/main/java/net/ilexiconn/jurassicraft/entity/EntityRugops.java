package net.ilexiconn.jurassicraft.entity;

import net.ilexiconn.jurassicraft.entity.base.EntityDinosaurAggressive;
import net.ilexiconn.llibrary.client.model.modelbase.ChainBuffer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class EntityRugops extends EntityDinosaurAggressive
{
    public ChainBuffer tailBuffer = new ChainBuffer(6);

    public EntityRugops(World world)
    {
        super(world);
        // this.tasks.addTask(0, new EntityAIAttackOnCollide(this, EntityPlayer.class, 0.5F, false));
        // this.targetTasks.addTask(0, new EntityAINearestAttackableTarget(this, EntityPlayer.class, false));

        // Placeholder AI

        if (!this.isChild())
        {
            this.tasks.addTask(0, new EntityAIAttackOnCollide(this, EntityPlayer.class, dinosaur.getAttackSpeed(), false));
            this.targetTasks.addTask(0, new EntityAINearestAttackableTarget(this, EntityPlayer.class, false));
            this.tasks.addTask(0, new EntityAIAttackOnCollide(this, EntityPig.class, dinosaur.getAttackSpeed(), false));
            this.targetTasks.addTask(0, new EntityAINearestAttackableTarget(this, EntityPig.class, false));

            this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, new Class[] { EntityPlayer.class }));

            this.tasks.addTask(6, new EntityAIWander(this, 1.0D));
            this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
            this.tasks.addTask(8, new EntityAILookIdle(this));
        }
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        
        if (this.isChild())
        {
            this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(dinosaur.getBabyHealth());
            this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(dinosaur.getBabySpeed());
            this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(dinosaur.getBabyStrength());
            this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(dinosaur.getBabyKnockback());
        }
        else
        {
            this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(dinosaur.getAdultHealth());
            this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(dinosaur.getAdultSpeed());
            this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(dinosaur.getAdultStrength());
            this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(dinosaur.getAdultKnockback());
        }
    }

    public void onUpdate()
    {
        this.tailBuffer.calculateChainSwingBuffer(68.0F, 5, 4.0F, this);
        super.onUpdate();
    }
}