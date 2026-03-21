CREATE CONSTRAINT canonicalProjection_projectionId_unique IF NOT EXISTS FOR (n:CanonicalProjection) REQUIRE n.projectionId IS UNIQUE;
