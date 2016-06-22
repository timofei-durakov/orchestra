package org.orchestra.config

/**
  * Created by tdurakov on 22/06/16.
  */

trait Periodic

final case class CheckEndpoints(period: Option[Double]) extends Periodic


