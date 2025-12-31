package br.com.paulocalderan.subscriptionservice.infrastructure.config;

import br.com.paulocalderan.subscriptionservice.infrastructure.scheduler.ProcessStaleProcessingSubscriptionsJob;
import br.com.paulocalderan.subscriptionservice.infrastructure.scheduler.RenewalJob;
import org.quartz.*;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;


@Configuration
public class QuartzSchedulerConfig extends SpringBeanJobFactory {

    private final ApplicationContext applicationContext;

    public QuartzSchedulerConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Object job = super.createJobInstance(bundle);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(job);
        return job;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(this);
        factory.setAutoStartup(true);
        return factory;
    }

    @Bean
    @DependsOn("schedulerFactoryBean")
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws Exception {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        
        JobDetail renewalJobDetail = JobBuilder.newJob(RenewalJob.class)
                .withIdentity("renewalJob")
                .withDescription("Job for automatic subscription renewal")
                .storeDurably()
                .build();

        Trigger renewalTrigger = TriggerBuilder.newTrigger()
                .forJob(renewalJobDetail)
                .withIdentity("renewalTrigger")
                .withDescription("Trigger to execute renewal daily at 00:00")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
                .build();

        if (!scheduler.checkExists(renewalJobDetail.getKey())) {
            scheduler.scheduleJob(renewalJobDetail, renewalTrigger);
        }

        JobDetail staleProcessingJobDetail = JobBuilder.newJob(ProcessStaleProcessingSubscriptionsJob.class)
                .withIdentity("staleProcessingSubscriptionsJob")
                .withDescription("Job to cancel subscriptions in PROCESSING status for more than 24 hours")
                .storeDurably()
                .build();

        Trigger staleProcessingTrigger = TriggerBuilder.newTrigger()
                .forJob(staleProcessingJobDetail)
                .withIdentity("staleProcessingTrigger")
                .withDescription("Trigger to execute stale processing cleanup every 8 hours")
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 */8 * * ?"))
                .build();

        if (!scheduler.checkExists(staleProcessingJobDetail.getKey())) {
            scheduler.scheduleJob(staleProcessingJobDetail, staleProcessingTrigger);
        }
        
        return scheduler;
    }
}

