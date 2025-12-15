CREATE SCHEMA "public";
CREATE TABLE "activities" (
	"id" serial PRIMARY KEY,
	"user_id" integer NOT NULL,
	"activity_type" varchar(30) NOT NULL,
	"target_user_id" integer,
	"collection_id" integer,
	"item_id" integer,
	"comment_id" integer,
	"metadata" jsonb,
	"is_read" boolean DEFAULT false,
	"created_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "activities_activity_type_check" CHECK (CHECK (((activity_type)::text = ANY ((ARRAY['follow'::character varying, 'like'::character varying, 'comment'::character varying, 'add_item'::character varying, 'create_collection'::character varying, 'update_collection'::character varying])::text[]))))
);
CREATE TABLE "collection_items" (
	"id" serial PRIMARY KEY,
	"collection_id" integer NOT NULL,
	"item_id" integer NOT NULL,
	"note" text,
	"rating" integer,
	"added_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "collection_items_collection_id_item_id_key" UNIQUE("collection_id","item_id"),
	CONSTRAINT "collection_items_rating_check" CHECK (CHECK (((rating >= 1) AND (rating <= 5))))
);
CREATE TABLE "collection_tags" (
	"collection_id" integer,
	"tag_id" integer,
	CONSTRAINT "collection_tags_pkey" PRIMARY KEY("collection_id","tag_id")
);
CREATE TABLE "collections" (
	"id" serial PRIMARY KEY,
	"name" varchar(255) NOT NULL,
	"description" text,
	"cover_image_url" text,
	"is_private" boolean DEFAULT false,
	"owner_id" integer NOT NULL,
	"view_count" integer DEFAULT 0,
	"like_count" integer DEFAULT 0,
	"comment_count" integer DEFAULT 0,
	"created_at" timestamp with time zone DEFAULT now(),
	"updated_at" timestamp with time zone DEFAULT now()
);
CREATE TABLE "comments" (
	"id" serial PRIMARY KEY,
	"user_id" integer NOT NULL,
	"content" text NOT NULL,
	"target_id" integer NOT NULL,
	"target_type" varchar(20) NOT NULL,
	"parent_id" integer,
	"like_count" integer DEFAULT 0,
	"created_at" timestamp with time zone DEFAULT now(),
	"updated_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "comments_target_type_check" CHECK (CHECK (((target_type)::text = ANY ((ARRAY['collection'::character varying, 'item'::character varying])::text[]))))
);
CREATE TABLE "items" (
	"id" serial PRIMARY KEY,
	"type" varchar(20) NOT NULL,
	"title" varchar(255) NOT NULL,
	"description" text,
	"cover_image_url" text,
	"external_id" varchar(255),
	"metadata" jsonb,
	"created_by" integer,
	"created_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "items_type_check" CHECK (CHECK (((type)::text = ANY ((ARRAY['book'::character varying, 'movie'::character varying, 'music'::character varying, 'artist'::character varying, 'game'::character varying, 'other'::character varying])::text[]))))
);
CREATE TABLE "likes" (
	"id" serial PRIMARY KEY,
	"user_id" integer NOT NULL UNIQUE,
	"target_id" integer NOT NULL UNIQUE,
	"target_type" varchar(20) NOT NULL UNIQUE,
	"created_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "likes_user_id_target_id_target_type_key" UNIQUE("user_id","target_id","target_type"),
	CONSTRAINT "likes_target_type_check" CHECK (CHECK (((target_type)::text = ANY ((ARRAY['collection'::character varying, 'item'::character varying, 'comment'::character varying])::text[]))))
);
CREATE TABLE "notifications" (
	"id" serial PRIMARY KEY,
	"recipient_id" integer NOT NULL,
	"notification_type" varchar(20) NOT NULL,
	"sender_id" integer NOT NULL,
	"target_id" integer NOT NULL,
	"target_type" varchar(20) NOT NULL,
	"is_read" boolean DEFAULT false,
	"created_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "notifications_notification_type_check" CHECK (CHECK (((notification_type)::text = ANY ((ARRAY['like'::character varying, 'comment'::character varying, 'follow'::character varying, 'mention'::character varying])::text[]))))
);
CREATE TABLE "tags" (
	"id" serial PRIMARY KEY,
	"name" varchar(50) NOT NULL CONSTRAINT "tags_name_key" UNIQUE
);
CREATE TABLE "user_follows" (
	"follower_id" integer,
	"following_id" integer,
	"created_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "user_follows_pkey" PRIMARY KEY("follower_id","following_id"),
	CONSTRAINT "user_follows_check" CHECK (CHECK ((follower_id <> following_id)))
);
CREATE TABLE "users" (
	"id" serial PRIMARY KEY,
	"username" varchar(50) NOT NULL CONSTRAINT "users_username_key" UNIQUE,
	"email" varchar(255) NOT NULL CONSTRAINT "users_email_key" UNIQUE,
	"password" varchar(255) NOT NULL,
	"display_name" varchar(100),
	"avatar_url" text,
	"bio" text,
	"created_at" timestamp with time zone DEFAULT now(),
	"updated_at" timestamp with time zone DEFAULT now()
);
ALTER TABLE "activities" ADD CONSTRAINT "activities_collection_id_fkey" FOREIGN KEY ("collection_id") REFERENCES "collections"("id") ON DELETE SET NULL;
ALTER TABLE "activities" ADD CONSTRAINT "activities_comment_id_fkey" FOREIGN KEY ("comment_id") REFERENCES "comments"("id") ON DELETE SET NULL;
ALTER TABLE "activities" ADD CONSTRAINT "activities_item_id_fkey" FOREIGN KEY ("item_id") REFERENCES "items"("id") ON DELETE SET NULL;
ALTER TABLE "activities" ADD CONSTRAINT "activities_target_user_id_fkey" FOREIGN KEY ("target_user_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "activities" ADD CONSTRAINT "activities_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "collection_items" ADD CONSTRAINT "collection_items_collection_id_fkey" FOREIGN KEY ("collection_id") REFERENCES "collections"("id") ON DELETE CASCADE;
ALTER TABLE "collection_items" ADD CONSTRAINT "collection_items_item_id_fkey" FOREIGN KEY ("item_id") REFERENCES "items"("id") ON DELETE CASCADE;
ALTER TABLE "collection_tags" ADD CONSTRAINT "collection_tags_collection_id_fkey" FOREIGN KEY ("collection_id") REFERENCES "collections"("id") ON DELETE CASCADE;
ALTER TABLE "collection_tags" ADD CONSTRAINT "collection_tags_tag_id_fkey" FOREIGN KEY ("tag_id") REFERENCES "tags"("id") ON DELETE CASCADE;
ALTER TABLE "collections" ADD CONSTRAINT "collections_owner_id_fkey" FOREIGN KEY ("owner_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "comments" ADD CONSTRAINT "comments_parent_id_fkey" FOREIGN KEY ("parent_id") REFERENCES "comments"("id") ON DELETE CASCADE;
ALTER TABLE "comments" ADD CONSTRAINT "comments_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "items" ADD CONSTRAINT "items_created_by_fkey" FOREIGN KEY ("created_by") REFERENCES "users"("id") ON DELETE SET NULL;
ALTER TABLE "likes" ADD CONSTRAINT "likes_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "notifications" ADD CONSTRAINT "notifications_recipient_id_fkey" FOREIGN KEY ("recipient_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "notifications" ADD CONSTRAINT "notifications_sender_id_fkey" FOREIGN KEY ("sender_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "user_follows" ADD CONSTRAINT "user_follows_follower_id_fkey" FOREIGN KEY ("follower_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "user_follows" ADD CONSTRAINT "user_follows_following_id_fkey" FOREIGN KEY ("following_id") REFERENCES "users"("id") ON DELETE CASCADE;
CREATE UNIQUE INDEX "activities_pkey" ON "activities" ("id");
CREATE INDEX "idx_activities_user" ON "activities" ("user_id","created_at");
CREATE UNIQUE INDEX "collection_items_collection_id_item_id_key" ON "collection_items" ("collection_id","item_id");
CREATE UNIQUE INDEX "collection_items_pkey" ON "collection_items" ("id");
CREATE UNIQUE INDEX "collection_tags_pkey" ON "collection_tags" ("collection_id","tag_id");
CREATE UNIQUE INDEX "collections_pkey" ON "collections" ("id");
CREATE INDEX "idx_collections_created_at" ON "collections" ("created_at");
CREATE INDEX "idx_collections_owner" ON "collections" ("owner_id");
CREATE UNIQUE INDEX "comments_pkey" ON "comments" ("id");
CREATE INDEX "idx_comments_target" ON "comments" ("target_id","target_type");
CREATE INDEX "idx_items_external_id" ON "items" ("external_id");
CREATE INDEX "idx_items_title" ON "items" ("title");
CREATE INDEX "idx_items_type" ON "items" ("type");
CREATE UNIQUE INDEX "items_pkey" ON "items" ("id");
CREATE INDEX "idx_likes_target" ON "likes" ("target_id","target_type");
CREATE UNIQUE INDEX "likes_pkey" ON "likes" ("id");
CREATE UNIQUE INDEX "likes_user_id_target_id_target_type_key" ON "likes" ("user_id","target_id","target_type");
CREATE INDEX "idx_notifications_recipient" ON "notifications" ("recipient_id","is_read","created_at");
CREATE UNIQUE INDEX "notifications_pkey" ON "notifications" ("id");
CREATE UNIQUE INDEX "tags_name_key" ON "tags" ("name");
CREATE UNIQUE INDEX "tags_pkey" ON "tags" ("id");
CREATE UNIQUE INDEX "user_follows_pkey" ON "user_follows" ("follower_id","following_id");
CREATE UNIQUE INDEX "users_email_key" ON "users" ("email");
CREATE UNIQUE INDEX "users_pkey" ON "users" ("id");
CREATE UNIQUE INDEX "users_username_key" ON "users" ("username");