-- Add image_prompts column to store AI-generated image prompts for each episode
-- These prompts will be used during the MEDIA stage to generate actual scene images

ALTER TABLE generation_episode_content
    ADD COLUMN image_prompts JSONB;

COMMENT ON COLUMN generation_episode_content.image_prompts IS 'AI-generated image prompts for scene illustrations. Format: [{"description": "...", "sceneContext": "..."}]';
