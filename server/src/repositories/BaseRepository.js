// src/repositories/BaseRepository.js
// Base Repository pattern - hoc tu Ktor Chat

class BaseRepository {
    constructor(model) {
        this.model = model;
    }

    async create(data) {
        try {
            const entity = new this.model(data);
            return await entity.save();
        } catch (error) {
            throw new Error(`Create failed: ${error.message}`);
        }
    }

    async findById(id) {
        try {
            return await this.model.findById(id);
        } catch (error) {
            throw new Error(`Find by ID failed: ${error.message}`);
        }
    }

    async findOne(query) {
        try {
            return await this.model.findOne(query);
        } catch (error) {
            throw new Error(`Find one failed: ${error.message}`);
        }
    }

    async findAll(query = {}, options = {}) {
        try {
            const { limit, skip, sort, populate } = options;
            
            let queryBuilder = this.model.find(query);
            
            if (limit) queryBuilder = queryBuilder.limit(limit);
            if (skip) queryBuilder = queryBuilder.skip(skip);
            if (sort) queryBuilder = queryBuilder.sort(sort);
            if (populate) queryBuilder = queryBuilder.populate(populate);
            
            return await queryBuilder;
        } catch (error) {
            throw new Error(`Find all failed: ${error.message}`);
        }
    }

    async update(id, data) {
        try {
            return await this.model.findByIdAndUpdate(
                id,
                data,
                { new: true, runValidators: true }
            );
        } catch (error) {
            throw new Error(`Update failed: ${error.message}`);
        }
    }

    async delete(id) {
        try {
            return await this.model.findByIdAndDelete(id);
        } catch (error) {
            throw new Error(`Delete failed: ${error.message}`);
        }
    }

    async count(query = {}) {
        try {
            return await this.model.countDocuments(query);
        } catch (error) {
            throw new Error(`Count failed: ${error.message}`);
        }
    }

    async exists(query) {
        try {
            return await this.model.exists(query);
        } catch (error) {
            throw new Error(`Exists check failed: ${error.message}`);
        }
    }
}

module.exports = BaseRepository;
