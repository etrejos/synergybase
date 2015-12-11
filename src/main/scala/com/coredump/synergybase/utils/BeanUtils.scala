package com.coredump.synergybase.util

import javax.management.ObjectName
import java.lang.management.ManagementFactory

/** Bean Utils. */
object BeanUtils {

  /** Removes an MBean
    *
    * Example: BeanUtils.removeMBean("my-bean")
    *
    * @param name name of the bean
    */
  def removeMBean(name: String): Unit =
    ManagementFactory.getPlatformMBeanServer().unregisterMBean(
      new ObjectName(name))
}
