/*
 * Copyright 2012 Daniel Bechler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.danielbechler.diff.helper;

import de.danielbechler.diff.*;
import de.danielbechler.diff.collection.*;
import de.danielbechler.diff.visitor.*;
import org.fest.assertions.api.*;
import org.fest.assertions.core.*;

/** @author Daniel Bechler */
public final class NodeAssertions
{
	public static Syntax.SelectNode assertThat(final DiffNode node)
	{
		return new NodeAssertionLanguage(node);
	}

	public static final class NodeAssertionLanguage implements Syntax.SelectNode, Syntax.AssertNode
	{
		private final DiffNode rootNode;

		private DiffNode selectedNode;
		private NodePath nodePath;

		private NodeAssertionLanguage(final DiffNode rootNode)
		{
			this.rootNode = rootNode;
		}

		public Syntax.AssertNode root()
		{
			this.selectedNode = rootNode;
			this.nodePath = NodePath.createBuilder().withRoot().build();
			return this;
		}

		public Syntax.AssertNode self()
		{
			this.selectedNode = rootNode;
			this.nodePath = rootNode.getPath();
			return this;
		}

		public Syntax.AssertNode child(final NodePath nodePath)
		{
			if (rootNode != null)
			{
				selectedNode = rootNode.getChild(nodePath);
			}
			this.nodePath = nodePath;
			return this;
		}

		public Syntax.AssertNode child(final NodePath.AppendableBuilder propertyPathBuilder)
		{
			return child(propertyPathBuilder.build());
		}

		public Syntax.AssertNode child(final String propertyName, final String... propertyNames)
		{
			return child(NodePath.buildWith(propertyName, propertyNames));
		}

		public Syntax.AssertNode child(final Element pathElement)
		{
			return child(NodePath.createBuilder().withRoot().withElement(pathElement));
		}

		public Syntax.AssertNode collectionChild(final Object referenceItem)
		{
			return child(new CollectionElement(referenceItem));
		}

		public Syntax.AssertNode doesExist()
		{
			Assertions.assertThat(rootNode).has(childAt(nodePath));
			return this;
		}

		public Syntax.AssertNode doesNotExist()
		{
			Assertions.assertThat(rootNode).has(noChildAt(nodePath));
			return this;
		}

		public Syntax.AssertNode hasState(final DiffNode.State state)
		{
			doesExist();
			Assertions.assertThat(selectedNode).has(state(state));
			return this;
		}

		public Syntax.AssertNode hasChildren()
		{
			doesExist();
			Assertions.assertThat(selectedNode).has(atLeastOneChild());
			return this;
		}

		public Syntax.AssertNode hasChildren(final int count)
		{
			doesExist();
			Assertions.assertThat(selectedNode).has(exactChildCountOf(count));
			return this;
		}

		private static Condition<DiffNode> childAt(final NodePath nodePath)
		{
			return new Condition<DiffNode>("child at path " + nodePath)
			{
				@Override
				public boolean matches(final DiffNode value)
				{
					if (value == null)
					{
						return false;
					}
					else
					{
						final PropertyVisitor visitor = new PropertyVisitor(nodePath);
						value.visit(visitor);
						return visitor.getNode() != null;
					}
				}
			};
		}

		private static Condition<DiffNode> state(final DiffNode.State state)
		{
			return new Condition<DiffNode>("state " + state)
			{
				@Override
				public boolean matches(final DiffNode value)
				{
					if (value == null)
					{
						return false;
					}
					else
					{
						return value.getState() == state;
					}
				}
			};
		}

		private static Condition<DiffNode> noChildAt(final NodePath nodePath)
		{
			return new Condition<DiffNode>("no child at path " + nodePath)
			{
				@Override
				public boolean matches(final DiffNode value)
				{
					if (value == null)
					{
						return true;
					}
					else
					{
						final PropertyVisitor visitor = new PropertyVisitor(nodePath);
						value.visit(visitor);
						return visitor.getNode() == null;
					}
				}
			};
		}

		private static Condition<DiffNode> atLeastOneChild()
		{
			return new Condition<DiffNode>("at least one child")
			{
				@Override
				public boolean matches(final DiffNode value)
				{
					return value.hasChildren();
				}
			};
		}

		private static Condition<DiffNode> exactChildCountOf(final int count)
		{
			if (count < 0)
			{
				throw new IllegalArgumentException("The number of expected children must be greater or equal to 0.");
			}
			return new Condition<DiffNode>(count + " children")
			{
				@Override
				public boolean matches(final DiffNode value)
				{
					if (count == 0)
					{
						return value == null || value.getChildren().isEmpty();
					}
					else
					{
						return value != null && value.getChildren().size() == count;
					}
				}
			};
		}

		public Syntax.AssertNode hasNoChildren()
		{
			return hasChildren(0);
		}

		public Syntax.AssertNode isCircular()
		{
			doesExist();
			Assertions.assertThat(selectedNode.isCircular()).isTrue();
			return this;
		}

		public Syntax.AssertNode isUntouched()
		{
			doesExist();
			Assertions.assertThat(selectedNode.getState()).isEqualTo(DiffNode.State.UNTOUCHED);
			return this;
		}

		public Syntax.AssertNode hasChanges()
		{
			doesExist();
			Assertions.assertThat(selectedNode.hasChanges()).isTrue();
			return this;
		}

		public Syntax.AssertNode hasCircularStartPathEqualTo(final NodePath nodePath)
		{
			doesExist();
			Assertions.assertThat(selectedNode.getCircleStartPath()).isEqualTo(nodePath);
			return this;
		}
	}

	private NodeAssertions()
	{
	}

	private interface Syntax
	{
		public interface SelectNode
		{
			AssertNode root();

			AssertNode self();

			AssertNode child(NodePath nodePath);

			AssertNode child(NodePath.AppendableBuilder propertyPathBuilder);

			AssertNode child(String propertyName, String... propertyPathElements);

			AssertNode child(Element pathElement);

			AssertNode collectionChild(Object referenceItem);
		}

		public interface AssertNode
		{
			AssertNode doesExist();

			AssertNode doesNotExist();

			AssertNode hasState(DiffNode.State state);

			AssertNode hasChildren();

			AssertNode hasChildren(int count);

			AssertNode hasNoChildren();

			AssertNode isCircular();

			AssertNode isUntouched();

			AssertNode hasChanges();

			AssertNode hasCircularStartPathEqualTo(final NodePath nodePath);
		}
	}
}
