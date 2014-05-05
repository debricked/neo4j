/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_1.pipes

import org.neo4j.cypher.internal.compiler.v2_1.{Comparer, ExecutionContext}
import org.neo4j.cypher.internal.compiler.v2_1.PlanDescription.Arguments.KeyNames

trait SortDescription {
  def id: String
}
case class Ascending(id:String) extends SortDescription
case class Descending(id:String) extends SortDescription

case class SortPipe(source: Pipe, orderBy: Seq[SortDescription])(implicit monitor: PipeMonitor)
  extends PipeWithSource(source, monitor) with Comparer {
  protected def internalCreateResults(input: Iterator[ExecutionContext], state: QueryState): Iterator[ExecutionContext] =
    input.toList.
      sortWith((a, b) => compareBy(a, b, orderBy)(state)).iterator

  def planDescription = source.planDescription.andThen(this, "Sort", KeyNames(orderBy.map(_.id)))

  def symbols = source.symbols

  override def isLazy = false

  private def compareBy(a: ExecutionContext, b: ExecutionContext, order: Seq[SortDescription])(implicit qtx: QueryState): Boolean = order match {
    case Nil => false
    case sort :: tail =>
      val column = sort.id
      val aVal = a(column)
      val bVal = b(column)

      Math.signum(compare(aVal, bVal)) match {
        case 1 => sort.isInstanceOf[Descending]
        case -1 => sort.isInstanceOf[Ascending]
        case 0 => compareBy(a, b, tail)
      }
  }
}
