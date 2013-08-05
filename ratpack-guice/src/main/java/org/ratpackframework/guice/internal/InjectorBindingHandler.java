/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.guice.internal;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.ratpackframework.guice.Guice;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.ObjectHoldingChildRegistry;
import org.ratpackframework.render.RenderController;
import org.ratpackframework.render.Renderer;
import org.ratpackframework.render.internal.DefaultRenderController;

import java.util.List;
import java.util.Map;

public class InjectorBindingHandler implements Handler {

  private final Injector injector;
  private final List<Handler> delegate;
  private final ImmutableList<Renderer<?>> renderers;

  public InjectorBindingHandler(Injector injector, Handler delegate) {
    this.injector = injector;
    this.delegate = ImmutableList.of(delegate);

    ImmutableList.Builder<Renderer<?>> renderersBuilder = ImmutableList.builder();
    Map<Key<?>, Binding<?>> allBindings = injector.getAllBindings();
    for (Map.Entry<Key<?>, Binding<?>> keyBindingEntry : allBindings.entrySet()) {
      Class<?> rawType = keyBindingEntry.getKey().getTypeLiteral().getRawType();
      if (Renderer.class.isAssignableFrom(rawType)) {
        Renderer<?> renderer = (Renderer) keyBindingEntry.getValue().getProvider().get();
        renderersBuilder.add(renderer);
      }
    }

    renderers = renderersBuilder.build();
  }

  public void handle(Context context) {
    Registry<Object> injectorRegistry = Guice.registry(context, injector);

    RenderController parentRenderController = context.maybeGet(RenderController.class);
    RenderController renderController = new DefaultRenderController(parentRenderController, renderers);

    Registry<Object> registry = new ObjectHoldingChildRegistry<Object>(injectorRegistry, RenderController.class, renderController);

    context.insert(registry, delegate);
  }
}
