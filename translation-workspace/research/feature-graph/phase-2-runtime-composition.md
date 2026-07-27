# Phase 2 shared runtime composition

Date: 2026-07-27

## Result

Production now has one Feature runtime composition that can accept independently installed Entry and application
Feature inputs. Entry Interactions no longer assembles the production graph.

## Ownership

The new `:feature-runtime` Android library is the neutral runtime boundary:

- `feature-graph` remains a pure graph kernel with no Android or Injekt dependencies.
- `feature-runtime` owns `FeatureRuntimeInputs`, `FeatureRuntimeComposition`, application Feature installation, runtime
  boundaries, and shared warmup coordination.
- `entry-interactions` installs Entry plugins and Feature modules, builds `EntryInteractions`, and returns
  `FeatureRuntimeInputs`.
- the app composition root combines Entry and application inputs and registers exactly one
  `FeatureRuntimeComposition`.

`EntryInteractionComposition` remains as a compatibility view for supported Entry callers and tests. It delegates all
graph, evaluation, artifact, and execution access to the shared composition and does not assemble another graph.

## Application Feature installation

An `ApplicationFeatureRuntimeModule` keeps an owner-local graph contributor and its runtime installation together.
Installed modules may contribute:

- application capability providers;
- specialized adapters and contract fixtures;
- transient and durable execution bindings;
- runtime boundaries;
- warmups.

The installer aggregates those artifacts into exactly one `ApplicationSubjectContribution`. The application subject is
present even when no application Feature module is installed, so adding the first module does not change the
composition shape.

Duplicate module IDs, graph contributors, capability IDs, and runtime boundary types fail deterministically.

## Production discovery

The app build scans owner-local `*.application-feature-module` descriptors from `main` and the current Android variant.
The cacheable generator validates descriptor IDs and qualified symbols, sorts modules deterministically, and emits
direct typed Kotlin references.

The generated topology is intentionally an empty typed list in Phase 2. The first real descriptor belongs beside the
Translation runtime module in Phase 4. A missing symbol or a symbol of the wrong type therefore fails Kotlin
compilation; there is no reflection, `ServiceLoader`, or manual central module list.

## Entry migration

Existing Entry Feature factories now inject `FeatureRuntimeComposition` for evaluation, artifacts, graph, and execution
state. Factories that also require Entry dispatch inject `EntryInteractions` separately. Entry host dependencies,
provider indexes, runtime boundary validation, image installers, and warmups retain their existing owners and behavior.

## Documentation

The developer architecture guide still describes the Entry-only composition. Its broader two-scope rewrite is
intentionally part of Phase 3, together with the application Feature report and generalized architecture gate.
